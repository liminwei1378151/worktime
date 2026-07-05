package com.example.worktime.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.worktime.data.repository.WorktimeRepository
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

class MainViewModel(
    private val repository: WorktimeRepository,
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
        repository.observeAllHistoryEvents()
    ) { plan, events, historyEvents ->
        BaseUiData(
            plan = plan,
            events = events,
            historyEvents = historyEvents
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
            historyEvents = baseData.historyEvents,
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

    fun savePlan(plan: ShiftPlan) {
        val validationError = plan.validate()
        if (validationError != null) {
            messageFlow.value = validationError
            return
        }
        viewModelScope.launch {
            isBusyFlow.value = true
            try {
                repository.savePlanAndRegenerateEvents(plan.copy(updatedAtEpochMillis = System.currentTimeMillis()))
                messageFlow.value = "方案已保存"
                widgetRefresher.refresh()
            } finally {
                isBusyFlow.value = false
            }
        }
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
}

private data class BaseUiData(
    val plan: ShiftPlan?,
    val events: List<ShiftEvent>,
    val historyEvents: List<PlanHistoryEvent>
)

class MainViewModelFactory(
    private val repository: WorktimeRepository,
    private val widgetRefresher: WorktimeWidgetRefresher
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, widgetRefresher) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
