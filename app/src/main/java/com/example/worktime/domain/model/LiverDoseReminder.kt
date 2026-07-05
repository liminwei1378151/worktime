package com.example.worktime.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class LiverDoseReminder(
    val remindAtEpochMillis: Long,
    val workStartAtEpochMillis: Long,
    val source: LiverDoseReminderSource
)

enum class LiverDoseReminderSource {
    MEAL_WINDOW,
    PRE_WORK_FALLBACK
}

object LiverDoseReminderPlanner {
    private val targetWorkStarts = setOf(
        LocalTime.of(0, 30),
        LocalTime.of(2, 30),
        LocalTime.of(4, 30)
    )

    private val mealWindows = listOf(
        Pair(LocalTime.of(6, 30), LocalTime.of(8, 30)),
        Pair(LocalTime.of(10, 30), LocalTime.of(12, 30)),
        Pair(LocalTime.of(16, 30), LocalTime.of(18, 30))
    )

    fun nextReminder(
        nowEpochMillis: Long,
        futureWorkEvents: List<ShiftEvent>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): LiverDoseReminder? {
        if (futureWorkEvents.size < 2) return null
        val sorted = futureWorkEvents.sortedBy { it.startAtEpochMillis }

        for (index in 0 until sorted.lastIndex) {
            val current = sorted[index]
            val next = sorted[index + 1]
            if (!isTargetNightShift(current, zoneId)) continue

            val mealReminderAt = pickMealBasedReminder(
                restStartAtEpochMillis = current.endAtEpochMillis,
                restEndAtEpochMillis = next.startAtEpochMillis,
                zoneId = zoneId
            )
            val remindAt = mealReminderAt ?: (next.startAtEpochMillis - Duration.ofMinutes(60).toMillis())

            if (remindAt <= nowEpochMillis) continue

            val source = if (mealReminderAt != null) {
                LiverDoseReminderSource.MEAL_WINDOW
            } else {
                LiverDoseReminderSource.PRE_WORK_FALLBACK
            }

            return LiverDoseReminder(
                remindAtEpochMillis = remindAt,
                workStartAtEpochMillis = next.startAtEpochMillis,
                source = source
            )
        }
        return null
    }

    private fun isTargetNightShift(event: ShiftEvent, zoneId: ZoneId): Boolean {
        val localStart = Instant.ofEpochMilli(event.startAtEpochMillis)
            .atZone(zoneId)
            .toLocalTime()
        return targetWorkStarts.contains(localStart)
    }

    private fun pickMealBasedReminder(
        restStartAtEpochMillis: Long,
        restEndAtEpochMillis: Long,
        zoneId: ZoneId
    ): Long? {
        if (restEndAtEpochMillis <= restStartAtEpochMillis) return null
        val restStart = Instant.ofEpochMilli(restStartAtEpochMillis).atZone(zoneId).toLocalDateTime()
        val restEnd = Instant.ofEpochMilli(restEndAtEpochMillis).atZone(zoneId).toLocalDateTime()

        var best: Long? = null
        var cursorDate = restStart.toLocalDate()
        val endDate = restEnd.toLocalDate()
        while (!cursorDate.isAfter(endDate)) {
            mealWindows.forEach { (windowStart, windowEnd) ->
                val overlapMidpoint = overlapMidpoint(cursorDate, windowStart, windowEnd, restStart, restEnd)
                if (overlapMidpoint != null) {
                    val epoch = overlapMidpoint.atZone(zoneId).toInstant().toEpochMilli()
                    if (best == null || epoch > best) {
                        best = epoch
                    }
                }
            }
            cursorDate = cursorDate.plusDays(1)
        }
        return best
    }

    private fun overlapMidpoint(
        date: LocalDate,
        windowStart: LocalTime,
        windowEnd: LocalTime,
        restStart: LocalDateTime,
        restEnd: LocalDateTime
    ): LocalDateTime? {
        val start = LocalDateTime.of(date, windowStart)
        val end = LocalDateTime.of(date, windowEnd)
        val overlapStart = if (start.isAfter(restStart)) start else restStart
        val overlapEnd = if (end.isBefore(restEnd)) end else restEnd
        if (!overlapStart.isBefore(overlapEnd)) return null

        val overlapMinutes = Duration.between(overlapStart, overlapEnd).toMinutes()
        return overlapStart.plusMinutes(overlapMinutes / 2)
    }
}
