package com.example.worktime.domain.planner

import com.example.worktime.domain.model.ShiftEvent
import com.example.worktime.domain.model.ShiftEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DailyRoutinePlannerTest {
    private val zoneId = ZoneId.systemDefault()

    @Test
    fun mealMatchedByShiftStart_isMovedEarlier() {
        val date = LocalDate.of(2026, 4, 14)
        val events = listOf(
            work("2026-04-14T08:00:00", "2026-04-14T10:00:00"),
            rest("2026-04-14T10:00:00", "2026-04-14T18:00:00")
        )

        val plan = DailyRoutinePlanner.planForDate(date, events, zoneId)

        assertEquals("07:20", format(plan.meals.first { it.type == MealType.BREAKFAST }.scheduledAtEpochMillis))
        assertEquals("12:00", format(plan.meals.first { it.type == MealType.LUNCH }.scheduledAtEpochMillis))
    }

    @Test
    fun mealNotMatchedByShiftStart_keepsBaseTime() {
        val date = LocalDate.of(2026, 4, 14)
        val events = listOf(
            work("2026-04-14T14:00:00", "2026-04-14T16:00:00")
        )

        val plan = DailyRoutinePlanner.planForDate(date, events, zoneId)

        assertEquals("08:00", format(plan.meals.first { it.type == MealType.BREAKFAST }.scheduledAtEpochMillis))
        assertEquals("12:00", format(plan.meals.first { it.type == MealType.LUNCH }.scheduledAtEpochMillis))
        assertEquals("18:00", format(plan.meals.first { it.type == MealType.DINNER }.scheduledAtEpochMillis))
    }

    @Test
    fun workDay_generatesPreShiftSupplements() {
        val date = LocalDate.of(2026, 4, 14)
        val events = listOf(
            work("2026-04-14T18:00:00", "2026-04-14T20:00:00")
        )

        val plan = DailyRoutinePlanner.planForDate(date, events, zoneId)

        assertTrue(plan.supplements.any { it.name == "党参生脉饮" && format(it.scheduledAtEpochMillis) == "17:35" })
        assertTrue(plan.supplements.any { it.name == "红枣" && format(it.scheduledAtEpochMillis) == "17:35" })
    }

    @Test
    fun longRestWindow_generatesSleepPlan() {
        val date = LocalDate.of(2026, 4, 14)
        val events = listOf(
            work("2026-04-14T00:00:00", "2026-04-14T02:00:00"),
            rest("2026-04-14T02:00:00", "2026-04-14T10:00:00"),
            work("2026-04-14T10:00:00", "2026-04-14T12:00:00")
        )

        val plan = DailyRoutinePlanner.planForDate(date, events, zoneId)

        assertTrue(plan.sleeps.isNotEmpty())
        assertEquals("02:30", format(plan.sleeps.first().startAtEpochMillis))
    }

    private fun work(start: String, end: String): ShiftEvent {
        return ShiftEvent(
            planId = 1L,
            type = ShiftEventType.WORK,
            startAtEpochMillis = epoch(start),
            endAtEpochMillis = epoch(end)
        )
    }

    private fun rest(start: String, end: String): ShiftEvent {
        return ShiftEvent(
            planId = 1L,
            type = ShiftEventType.REST,
            startAtEpochMillis = epoch(start),
            endAtEpochMillis = epoch(end)
        )
    }

    private fun epoch(value: String): Long {
        return LocalDateTime.parse(value).atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun format(epochMillis: Long): String {
        val time = java.time.Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalTime()
        return String.format("%02d:%02d", time.hour, time.minute)
    }
}
