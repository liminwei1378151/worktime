package com.example.worktime.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepScheduleTest {

    @Test
    fun fixedRuleMapping_returnsExpectedSlots() {
        assertSlot(12, 30, "20:30", "22:00")
        assertSlot(22, 30, "00:45", "07:45")
        assertSlot(18, 30, "21:00", "04:00")
        assertSlot(4, 30, "07:00", "10:00")
        assertSlot(14, 30, "22:00", "23:30")
        assertSlot(0, 30, "02:45", "09:45")
        assertSlot(20, 30, "22:45", "05:45")
        assertSlot(6, 30, "09:00", "12:00")
        assertSlot(16, 30, "19:00", "02:00")
        assertSlot(2, 30, "05:00", "08:00")
    }

    @Test
    fun noSleepRule_returnsNoSleepFlag() {
        assertNoSleep(8, 30)
        assertNoSleep(10, 30)
    }

    private fun assertSlot(workStartHour: Int, workStartMinute: Int, expectedStart: String, expectedEnd: String) {
        val schedule = SleepSchedule.calculate(workStartHour, workStartMinute)
        assertNotNull(schedule)
        val value = schedule!!
        assertFalse(value.noSleepThisRound)
        assertEquals(1, value.sleepSlots.size)
        val slot = value.sleepSlots.first()
        assertEquals(expectedStart, slot.startTime.toString().substring(0, 5))
        assertEquals(expectedEnd, slot.endTime.toString().substring(0, 5))
    }

    private fun assertNoSleep(workStartHour: Int, workStartMinute: Int) {
        val schedule = SleepSchedule.calculate(workStartHour, workStartMinute)
        assertNotNull(schedule)
        val value = schedule!!
        assertTrue(value.noSleepThisRound)
        assertTrue(value.sleepSlots.isEmpty())
    }
}
