package com.example.worktime.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyActivityTest {

    @Test
    fun screenshotRules_matchExpectedLabels() {
        assertEquals(listOf("cf"), DailyActivity.getActivitiesForWork(16, 30))
        assertEquals(listOf("sj"), DailyActivity.getActivitiesForWork(12, 30))
        assertEquals(listOf("tj"), DailyActivity.getActivitiesForWork(18, 30))
        assertEquals(listOf("qc"), DailyActivity.getActivitiesForWork(14, 30))
        assertEquals(listOf("sj"), DailyActivity.getActivitiesForWork(22, 30))
        assertEquals(listOf("cf"), DailyActivity.getActivitiesForWork(10, 30))
        assertEquals(listOf("qc", "xs", "tj", "cf"), DailyActivity.getActivitiesForWork(6, 30))
    }

    @Test
    fun screenshotRules_noLabelTimeSlotsReturnEmpty() {
        assertEquals(emptyList<String>(), DailyActivity.getActivitiesForWork(0, 30))
        assertEquals(emptyList<String>(), DailyActivity.getActivitiesForWork(2, 30))
        assertEquals(emptyList<String>(), DailyActivity.getActivitiesForWork(4, 30))
        assertEquals(emptyList<String>(), DailyActivity.getActivitiesForWork(8, 30))
        assertEquals(emptyList<String>(), DailyActivity.getActivitiesForWork(20, 30))
    }
}
