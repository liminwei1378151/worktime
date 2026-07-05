package com.example.worktime.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class LiverDoseReminderPlannerTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun startAt0030_prefersMealWindowInRest() {
        val now = toEpochMillis("2026-04-14T00:00:00")
        val events = listOf(
            workEvent("2026-04-14T00:30:00", "2026-04-14T02:30:00"),
            workEvent("2026-04-14T10:30:00", "2026-04-14T12:30:00")
        )

        val reminder = LiverDoseReminderPlanner.nextReminder(now, events, zoneId)

        assertEquals(toEpochMillis("2026-04-14T07:30:00"), reminder?.remindAtEpochMillis)
    }

    @Test
    fun startAt0230_prefersLatestMealWindowInRest() {
        val now = toEpochMillis("2026-04-14T01:00:00")
        val events = listOf(
            workEvent("2026-04-14T02:30:00", "2026-04-14T04:30:00"),
            workEvent("2026-04-14T12:30:00", "2026-04-14T14:30:00")
        )

        val reminder = LiverDoseReminderPlanner.nextReminder(now, events, zoneId)

        assertEquals(toEpochMillis("2026-04-14T11:30:00"), reminder?.remindAtEpochMillis)
    }

    @Test
    fun startAt0430_prefersLatestMealWindowInRest() {
        val now = toEpochMillis("2026-04-14T03:00:00")
        val events = listOf(
            workEvent("2026-04-14T04:30:00", "2026-04-14T06:30:00"),
            workEvent("2026-04-14T14:30:00", "2026-04-14T16:30:00")
        )

        val reminder = LiverDoseReminderPlanner.nextReminder(now, events, zoneId)

        assertEquals(toEpochMillis("2026-04-14T11:30:00"), reminder?.remindAtEpochMillis)
    }

    @Test
    fun nonTargetNightShift_returnsNull() {
        val now = toEpochMillis("2026-04-14T05:00:00")
        val events = listOf(
            workEvent("2026-04-14T06:30:00", "2026-04-14T08:30:00"),
            workEvent("2026-04-14T16:30:00", "2026-04-14T18:30:00")
        )

        val reminder = LiverDoseReminderPlanner.nextReminder(now, events, zoneId)

        assertNull(reminder)
    }

    private fun workEvent(start: String, end: String): ShiftEvent {
        return ShiftEvent(
            planId = 1L,
            type = ShiftEventType.WORK,
            startAtEpochMillis = toEpochMillis(start),
            endAtEpochMillis = toEpochMillis(end)
        )
    }

    private fun toEpochMillis(value: String): Long {
        return LocalDateTime.parse(value).atZone(zoneId).toInstant().toEpochMilli()
    }
}
