package com.example.worktime.ui

import com.example.worktime.data.local.settings.SettingsStore
import com.example.worktime.domain.model.AlarmMapping
import com.example.worktime.domain.model.LiverDoseReminder
import com.example.worktime.domain.model.LiverDoseReminderPlanner
import com.example.worktime.domain.model.PlanHistoryEvent
import com.example.worktime.domain.model.ShiftEvent
import com.example.worktime.domain.model.ShiftEventType
import com.example.worktime.domain.model.ShiftPlan
import com.example.worktime.domain.model.SleepSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class MainUiState(
    val plan: ShiftPlan? = null,
    val events: List<ShiftEvent> = emptyList(),
    val alarmMappings: List<AlarmMapping> = emptyList(),
    val historyEvents: List<PlanHistoryEvent> = emptyList(),
    val defaultRemindMinutes: Int = SettingsStore.DEFAULT_REMIND_MINUTES,
    val selectedMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val nowEpochMillis: Long = System.currentTimeMillis(),
    val isBusy: Boolean = false,
    val message: String? = null
) {
    val workEvents: List<ShiftEvent>
        get() = events.filter { it.type == ShiftEventType.WORK }

    val nextWorkEvent: ShiftEvent?
        get() = workEvents.firstOrNull { it.startAtEpochMillis >= nowEpochMillis }

    val nextAlarmMapping: AlarmMapping?
        get() = alarmMappings.firstOrNull { it.triggerAtEpochMillis >= nowEpochMillis }

    val remainingWorkCount: Int
        get() = workEvents.count { it.startAtEpochMillis >= nowEpochMillis }

    val remainingRestCount: Int
        get() = events.filter { it.type == ShiftEventType.REST }.count { it.startAtEpochMillis >= nowEpochMillis }

    val isEnded: Boolean
        get() = plan?.isEnded(nowEpochMillis) == true

    private fun isNightShift(startAtEpochMillis: Long): Boolean {
        val localTime = Instant.ofEpochMilli(startAtEpochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        val hour = localTime.hour
        return hour >= 0 && hour < 6
    }

    val nightShiftEvents: List<ShiftEvent>
        get() = workEvents.filter { isNightShift(it.startAtEpochMillis) }

    val remainingNightShiftCount: Int
        get() = nightShiftEvents.count { it.startAtEpochMillis >= nowEpochMillis }

    val nightShiftDates: Set<LocalDate>
        get() = nightShiftEvents.map {
            Instant.ofEpochMilli(it.startAtEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }.toSet()

    val nextSleepSchedule: SleepSchedule?
        get() = nextWorkEvent?.toSleepSchedule()

    val recentSleepSchedule: SleepSchedule?
        get() = workEvents
            .lastOrNull { it.endAtEpochMillis <= nowEpochMillis }
            ?.toSleepSchedule()

    val nextLiverDoseReminder: LiverDoseReminder?
        get() = LiverDoseReminderPlanner.nextReminder(
            nowEpochMillis = nowEpochMillis,
            futureWorkEvents = workEvents
        )

    private fun ShiftEvent.toSleepSchedule(): SleepSchedule? {
        val localTime = Instant.ofEpochMilli(startAtEpochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        return SleepSchedule.calculate(localTime.hour, localTime.minute)
    }
}
