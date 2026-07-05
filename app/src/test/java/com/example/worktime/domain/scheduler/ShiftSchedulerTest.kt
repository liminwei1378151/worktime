package com.example.worktime.domain.scheduler

import com.example.worktime.domain.model.ShiftEventType
import com.example.worktime.domain.model.ShiftPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ShiftSchedulerTest {
    private val scheduler = ShiftScheduler()
    private val zoneId = ZoneId.systemDefault()

    @Test
    fun oneDayPlan_generatesAlternatingEventsWithinBoundary() {
        val plan = ShiftPlan(
            totalDays = 1,
            firstWorkStartAtEpochMillis = toEpochMillis("2026-04-14T08:00:00"),
            silentRemindBeforeMinutes = 15,
            departRemindBeforeMinutes = 30
        )

        val events = scheduler.generate(plan)

        assertTrue(events.isNotEmpty())
        assertEquals(ShiftEventType.WORK, events[0].type)
        for (i in 1 until events.size) {
            val expected = if (events[i - 1].type == ShiftEventType.WORK) ShiftEventType.REST else ShiftEventType.WORK
            assertEquals(expected, events[i].type)
        }
        assertEquals(toEpochMillis("2026-04-15T12:30:00"), events.last().endAtEpochMillis)
    }

    @Test
    fun remindBefore_canCrossToPreviousDay() {
        val plan = ShiftPlan(
            totalDays = 1,
            firstWorkStartAtEpochMillis = toEpochMillis("2026-04-14T01:00:00"),
            silentRemindBeforeMinutes = 120,
            departRemindBeforeMinutes = 30
        )

        val firstWork = scheduler.generate(plan).first()

        assertEquals(toEpochMillis("2026-04-13T23:00:00"), firstWork.silentAlarmTriggerAtEpochMillis)
        assertEquals(toEpochMillis("2026-04-14T00:30:00"), firstWork.departAlarmTriggerAtEpochMillis)
    }

    @Test
    fun startInPast_futureWorkCanBeFilteredByConsumer() {
        val plan = ShiftPlan(
            totalDays = 3,
            firstWorkStartAtEpochMillis = toEpochMillis("2026-04-10T08:00:00"),
            silentRemindBeforeMinutes = 30,
            departRemindBeforeMinutes = 45
        )

        val events = scheduler.generate(plan)
        val boundary = toEpochMillis("2026-04-12T00:00:00")
        val futureWorkStarts = events.filter { it.type == ShiftEventType.WORK && it.startAtEpochMillis >= boundary }

        assertTrue(futureWorkStarts.isNotEmpty())
    }

    @Test
    fun invalidPlan_throwsException() {
        val plan = ShiftPlan(
            totalDays = 0,
            firstWorkStartAtEpochMillis = toEpochMillis("2026-04-14T08:00:00"),
            silentRemindBeforeMinutes = 15,
            departRemindBeforeMinutes = 30
        )

        try {
            scheduler.generate(plan)
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun toEpochMillis(value: String): Long {
        return LocalDateTime.parse(value).atZone(zoneId).toInstant().toEpochMilli()
    }
}
