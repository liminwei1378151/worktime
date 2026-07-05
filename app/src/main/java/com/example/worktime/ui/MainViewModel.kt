package com.example.worktime.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.worktime.alarm.manager.WorkAlarmScheduler
import com.example.worktime.data.local.settings.SettingsStore
import com.example.worktime.data.repository.WorktimeRepository
import com.example.worktime.domain.model.AlarmMapping
import com.example.worktime.domain.model.AlarmType
import com.example.worktime.domain.model.PlanHistoryEvent
import com.example.worktime.domain.model.ShiftEvent
import com.example.worktime.domain.model.ShiftPlan
import com.example.worktime.widget.WorktimeWidgetRefresher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import kotlin.math.abs

class MainViewModel(
    private val repository: WorktimeRepository,
    private val settingsStore: SettingsStore,
    private val alarmScheduler: WorkAlarmScheduler,
    private val widgetRefresher: WorktimeWidgetRefresher
) : ViewModel() {

    private val selectedMonthFlow = MutableStateFlow(YearMonth.now())
    private val selectedDateFlow = MutableStateFlow(java.time.LocalDate.now())
    private val nowFlow = MutableStateFlow(System.currentTimeMillis())
    private val isBusyFlow = MutableStateFlow(false)
    private val messageFlow = MutableStateFlow<String?>(null)

    private val baseDataFlow = combine(
        repository.observePlan(),
        repository.observeEvents(),
        repository.observeAlarmMappings(),
        repository.observeAllHistoryEvents(),
        settingsStore.defaultRemindMinutes
    ) { plan, events, alarmMappings, historyEvents, defaultRemindMinutes ->
        BaseUiData(
            plan = plan,
            events = events,
            alarmMappings = alarmMappings,
            historyEvents = historyEvents,
            defaultRemindMinutes = defaultRemindMinutes
        )
    }

    private val baseUiStateFlow = combine(
        baseDataFlow,
        selectedMonthFlow,
        selectedDateFlow,
        nowFlow
    ) { baseData, selectedMonth, selectedDate, now ->
        MainUiState(
            plan = baseData.plan,
            events = baseData.events,
            alarmMappings = baseData.alarmMappings,
            historyEvents = baseData.historyEvents,
            defaultRemindMinutes = baseData.defaultRemindMinutes,
            selectedMonth = selectedMonth,
            selectedDate = selectedDate,
            nowEpochMillis = now,
            isBusy = false,
            message = null
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        baseUiStateFlow,
        isBusyFlow,
        messageFlow
    ) { baseState, isBusy, message ->
        baseState.copy(isBusy = isBusy, message = message)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState()
    )

    init {
        widgetRefresher.refresh()
        viewModelScope.launch {
            while (true) {
                nowFlow.value = System.currentTimeMillis()
                delay(60_000)
            }
        }
    }

    fun selectDate(date: java.time.LocalDate) {
        selectedDateFlow.value = date
    }

    fun changeMonth(offset: Long) {
        selectedMonthFlow.value = selectedMonthFlow.value.plusMonths(offset)
    }

    fun consumeMessage() {
        messageFlow.value = null
    }

    fun showMessage(message: String) {
        messageFlow.value = message
    }

    fun saveDefaultRemindMinutes(minutes: Int) {
        if (minutes < 0) {
            messageFlow.value = "默认提前提醒不能小于 0 分钟"
            return
        }
        viewModelScope.launch {
            settingsStore.setDefaultRemindMinutes(minutes)
            messageFlow.value = "默认提前提醒已保存"
        }
    }

    fun savePlan(plan: ShiftPlan, createAlarmsAfterSave: Boolean) {
        val validationError = plan.validate()
        if (validationError != null) {
            messageFlow.value = validationError
            return
        }
        viewModelScope.launch {
            isBusyFlow.value = true
            try {
                val cleared = clearAllAlarms()
                repository.savePlanAndRegenerateEvents(plan.copy(updatedAtEpochMillis = System.currentTimeMillis()))
                if (createAlarmsAfterSave) {
                    val result = scheduleFutureAlarms()
                    messageFlow.value = "方案已保存，${result.message}"
                } else {
                    messageFlow.value = if (cleared > 0) {
                        "方案已保存，已尝试清理 $cleared 个系统闹钟"
                    } else {
                        "方案已保存"
                    }
                }
                widgetRefresher.refresh()
            } finally {
                isBusyFlow.value = false
            }
        }
    }

    fun createAlarms() {
        viewModelScope.launch {
            isBusyFlow.value = true
            try {
                val result = scheduleFutureAlarms()
                messageFlow.value = result.message
                widgetRefresher.refresh()
            } finally {
                isBusyFlow.value = false
            }
        }
    }

    fun rebuildAlarms() {
        viewModelScope.launch {
            isBusyFlow.value = true
            try {
                val cleared = clearAllAlarms()
                val result = scheduleFutureAlarms()
                messageFlow.value = if (result.created > 0) {
                    "已重建系统闹钟：尝试清理 $cleared 个，${result.message}"
                } else {
                    "已尝试清理 $cleared 个系统闹钟，${result.message}"
                }
                widgetRefresher.refresh()
            } finally {
                isBusyFlow.value = false
            }
        }
    }

    private suspend fun scheduleFutureAlarms(): AlarmScheduleSummary {
        val now = System.currentTimeMillis()
        val plan = repository.getPlan()
        if (plan == null) {
            messageFlow.value = "请先保存倒班方案"
            return AlarmScheduleSummary(created = 0, message = "请先保存倒班方案")
        }

        val futureWorkEvents = repository.getFutureWorkEvents(now)
        if (futureWorkEvents.isEmpty()) {
            repository.replaceAlarmMappings(emptyList())
            return AlarmScheduleSummary(created = 0, message = "当前无可创建的未来闹钟")
        }

        val event = futureWorkEvents.first()
        val silentRequestCode = requestCodeForEvent(event, AlarmType.SILENT)
        val departRequestCode = requestCodeForEvent(event, AlarmType.DEPART)
        var createdCount = 0

        val silentTriggerAt = event.silentAlarmTriggerAtEpochMillis?.let { maxOf(now + 2_000L, it) }
            ?: event.startAtEpochMillis - (plan.silentRemindBeforeMinutes * 60_000L)
        val departTriggerAt = event.departAlarmTriggerAtEpochMillis?.let { maxOf(now + 2_000L, it) }
            ?: event.startAtEpochMillis - (plan.departRemindBeforeMinutes * 60_000L)

        val silentResult = alarmScheduler.schedule(
            requestCode = silentRequestCode,
            triggerAtEpochMillis = silentTriggerAt,
            workStartAtEpochMillis = event.startAtEpochMillis,
            vibrate = true,
            label = "静音"
        )

        if (silentResult == WorkAlarmScheduler.ScheduleResult.CREATED_SILENT || silentResult == WorkAlarmScheduler.ScheduleResult.OPENED_CLOCK_UI) {
            createdCount++
        }

        val departResult = alarmScheduler.schedule(
            requestCode = departRequestCode,
            triggerAtEpochMillis = departTriggerAt,
            workStartAtEpochMillis = event.startAtEpochMillis,
            vibrate = true,
            label = "出发"
        )

        if (departResult == WorkAlarmScheduler.ScheduleResult.CREATED_SILENT || departResult == WorkAlarmScheduler.ScheduleResult.OPENED_CLOCK_UI) {
            createdCount++
        }

        if (silentResult == WorkAlarmScheduler.ScheduleResult.NO_CLOCK_APP && departResult == WorkAlarmScheduler.ScheduleResult.NO_CLOCK_APP) {
            repository.replaceAlarmMappings(emptyList())
            return AlarmScheduleSummary(created = 0, message = "未找到可用的系统时钟应用")
        }
        if (silentResult == WorkAlarmScheduler.ScheduleResult.FAILED && departResult == WorkAlarmScheduler.ScheduleResult.FAILED) {
            repository.replaceAlarmMappings(emptyList())
            return AlarmScheduleSummary(created = 0, message = "调用系统时钟失败，请手动检查系统闹钟应用")
        }

        val mappings = mutableListOf<AlarmMapping>()
        if (silentResult != WorkAlarmScheduler.ScheduleResult.NO_CLOCK_APP && silentResult != WorkAlarmScheduler.ScheduleResult.FAILED) {
            mappings += AlarmMapping(
                planId = plan.id,
                eventId = event.id,
                requestCode = silentRequestCode,
                alarmType = AlarmType.SILENT,
                workStartAtEpochMillis = event.startAtEpochMillis,
                triggerAtEpochMillis = silentTriggerAt
            )
        }
        if (departResult != WorkAlarmScheduler.ScheduleResult.NO_CLOCK_APP && departResult != WorkAlarmScheduler.ScheduleResult.FAILED) {
            mappings += AlarmMapping(
                planId = plan.id,
                eventId = event.id,
                requestCode = departRequestCode,
                alarmType = AlarmType.DEPART,
                workStartAtEpochMillis = event.startAtEpochMillis,
                triggerAtEpochMillis = departTriggerAt
            )
        }
        repository.replaceAlarmMappings(mappings)

        val message = buildString {
            if (mappings.any { it.alarmType == AlarmType.SILENT }) append("静音闹钟")
            if (mappings.any { it.alarmType == AlarmType.SILENT } && mappings.any { it.alarmType == AlarmType.DEPART }) append("和")
            if (mappings.any { it.alarmType == AlarmType.DEPART }) append("出发闹钟")
            if (createdCount == 2) append("已创建")
            else if (createdCount == 1) append("已创建")
            else append("创建失败，请手动在系统闹钟应用创建")
        }
        return AlarmScheduleSummary(created = createdCount, message = message)
    }

    private suspend fun clearAllAlarms(): Int {
        val existing = repository.getAlarmMappings()
        existing.forEach { mapping ->
            alarmScheduler.cancel(
                requestCode = mapping.requestCode,
                triggerAtEpochMillis = mapping.triggerAtEpochMillis,
                workStartAtEpochMillis = mapping.workStartAtEpochMillis,
                label = mapping.alarmType.name.lowercase()
            )
        }
        repository.replaceAlarmMappings(emptyList())
        return existing.size
    }

    fun updateHistoryEvent(id: Long, startAtEpochMillis: Long, endAtEpochMillis: Long) {
        viewModelScope.launch {
            try {
                repository.updateHistoryEvent(id, startAtEpochMillis, endAtEpochMillis)
                messageFlow.value = "历史记录已更新"
            } catch (e: Exception) {
                messageFlow.value = "更新失败：${e.localizedMessage}"
            }
        }
    }

    fun deleteHistoryEventsByDate(date: java.time.LocalDate) {
        viewModelScope.launch {
            try {
                repository.deleteHistoryEventsByDate(date)
                messageFlow.value = "当天历史记录已删除"
            } catch (e: Exception) {
                messageFlow.value = "删除失败：${e.localizedMessage}"
            }
        }
    }

    private fun requestCodeForEvent(event: ShiftEvent, type: AlarmType): Int {
        val combined = event.startAtEpochMillis xor event.endAtEpochMillis xor type.ordinal.toLong()
        val nonZero = (combined and 0x7FFF_FFFFL).toInt()
        return if (nonZero == 0) abs(event.startAtEpochMillis.toInt()) + 1 + type.ordinal else nonZero
    }
}

private data class AlarmScheduleSummary(
    val created: Int,
    val message: String
)

private data class BaseUiData(
    val plan: ShiftPlan?,
    val events: List<ShiftEvent>,
    val alarmMappings: List<AlarmMapping>,
    val historyEvents: List<PlanHistoryEvent>,
    val defaultRemindMinutes: Int
)

class MainViewModelFactory(
    private val repository: WorktimeRepository,
    private val settingsStore: SettingsStore,
    private val alarmScheduler: WorkAlarmScheduler,
    private val widgetRefresher: WorktimeWidgetRefresher
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, settingsStore, alarmScheduler, widgetRefresher) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
