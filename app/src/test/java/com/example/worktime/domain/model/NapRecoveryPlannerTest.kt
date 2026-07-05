package com.example.worktime.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class NapRecoveryPlannerTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun startAt0030_returnsMainAndNap() {
        val now = toEpochMillis("2026-04-14T00:00:00")
        val events = listOf(
            workEvent("2026-04-14T00:30:00", "2026-04-14T02:30:00"),
            workEvent("2026-04-14T10:30:00", "2026-04-14T12:30:00")
        )

        val schedule = NapRecoveryPlanner.nextSchedule(now, events, zoneId)

        assertEquals(toEpochMillis("2026-04-14T03:00:00"), schedule?.mainStartAtEpochMillis)
        assertEquals(toEpochMillis("2026-04-14T08:00:00"), schedule?.mainEndAtEpochMillis)
        assertEquals(toEpochMillis("2026-04-14T08:30:00"), schedule?.napStartAtEpochMillis)
        assertEquals(toEpochMillis("2026-04-14T09:30:00"), schedule?.napEndAtEpochMillis)
    }

    @Test
    fun startAt0230_returnsMainAndNap() {
        val now = toEpochMillis("2026-04-14T01:00:00")
        val events = listOf(
            workEvent("2026-04-14T02:30:00", "2026-04-14T04:30:00"),
            workEvent("2026-04-14T12:30:00", "2026-04-14T14:30:00")
        )

        val schedule = NapRecoveryPlanner.nextSchedule(now, events, zoneId)

        assertEquals(toEpochMillis("2026-04-14T05:00:00"), schedule?.mainStartAtEpochMillis)
        assertEquals(toEpochMillis("2026-04-14T10:00:00"), schedule?.mainEndAtEpochMillis)
        assertEquals(toEpochMillis("2026-04-14T10:30:00"), schedule?.napStartAtEpochMillis)
        assertEquals(toEpochMillis("2026-04-14T11:30:00"), schedule?.napEndAtEpochMillis)
    }

    @Test
    fun nonTargetStart_returnsNull() {
        val now = toEpochMillis("2026-04-14T01:00:00")
        val events = listOf(
            workEvent("2026-04-14T06:30:00", "2026-04-14T08:30:00"),
            workEvent("2026-04-14T16:30:00", "2026-04-14T18:30:00")
        )

        val schedule = NapRecoveryPlanner.nextSchedule(now, events, zoneId)

        assertNull(schedule)
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
