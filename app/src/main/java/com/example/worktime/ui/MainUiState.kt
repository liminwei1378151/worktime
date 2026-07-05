package com.example.worktime.ui

import com.example.worktime.domain.model.PlanHistoryEvent
import com.example.worktime.domain.model.ShiftEvent
import com.example.worktime.domain.model.ShiftEventType
import com.example.worktime.domain.model.ShiftPlan
import com.example.worktime.domain.planner.DailyRoutinePlan
import com.example.worktime.domain.planner.DailyRoutinePlanner
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class MainUiState(
    val plan: ShiftPlan? = null,
    val events: List<ShiftEvent> = emptyList(),
    val historyEvents: List<PlanHistoryEvent> = emptyList(),
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

    val selectedDateRoutinePlan: DailyRoutinePlan
        get() = DailyRoutinePlanner.planForDate(
            date = selectedDate,
            events = events,
            zoneId = ZoneId.systemDefault()
        )
}
