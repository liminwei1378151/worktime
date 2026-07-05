package com.example.worktime.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyActivityTest {

    @Test
    fun screenshotRules_matchExpectedLabels() {
        assertEquals(listOf("qc", "tj", "cf"), DailyActivity.getActivitiesForWork(6, 0))
        assertEquals(listOf("cf"), DailyActivity.getActivitiesForWork(10, 0))
        assertEquals(listOf("sj"), DailyActivity.getActivitiesForWork(12, 0))
        assertEquals(listOf("qc"), DailyActivity.getActivitiesForWork(14, 0))
        assertEquals(listOf("cf"), DailyActivity.getActivitiesForWork(16, 0))
        assertEquals(listOf("tj"), DailyActivity.getActivitiesForWork(18, 0))
        assertEquals(listOf("xs", "sj"), DailyActivity.getActivitiesForWork(22, 0))
    }

    @Test
    fun screenshotRules_noLabelTimeSlotsReturnEmpty() {
        assertEquals(emptyList<String>(), DailyActivity.getActivitiesForWork(0, 0))
        assertEquals(emptyList<String>(), DailyActivity.getActivitiesForWork(2, 0))
        assertEquals(emptyList<String>(), DailyActivity.getActivitiesForWork(4, 0))
        assertEquals(emptyList<String>(), DailyActivity.getActivitiesForWork(8, 0))
        assertEquals(emptyList<String>(), DailyActivity.getActivitiesForWork(20, 0))
    }
}
