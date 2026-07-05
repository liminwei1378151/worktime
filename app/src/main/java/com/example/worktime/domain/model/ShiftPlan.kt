package com.example.worktime.domain.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

data class ShiftPlan(
    val id: Long = SINGLE_PLAN_ID,
    val totalDays: Int,
    val firstWorkStartAtEpochMillis: Long,
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
) {
    val workDurationMinutes: Int = 2 * 60
    val restDurationMinutes: Int = 8 * 60

    fun validate(): String? {
        if (totalDays <= 0) return "总天数必须大于 0"
        return null
    }

    fun calculateEndAtEpochMillis(): Long {
        val workCycleMillis = Duration.ofMinutes((workDurationMinutes + restDurationMinutes).toLong()).toMillis()
        val totalMillis = workCycleMillis * totalDays
        val baseDateTime = Instant.ofEpochMilli(firstWorkStartAtEpochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val lastDayDate = baseDateTime.toLocalDate().plusDays(totalDays.toLong())
        val endDateTime = java.time.LocalDateTime.of(lastDayDate, java.time.LocalTime.of(12, 30))
        return endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun isEnded(nowEpochMillis: Long): Boolean {
        return nowEpochMillis >= calculateEndAtEpochMillis()
    }

    fun totalDurationMillis(): Long {
        val workCycleMillis = Duration.ofMinutes((workDurationMinutes + restDurationMinutes).toLong()).toMillis()
        return workCycleMillis * totalDays
    }

    companion object {
        const val SINGLE_PLAN_ID: Long = 1L
    }
}
