package com.example.worktime.domain.model

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class NapRecoverySchedule(
    val mainStartAtEpochMillis: Long,
    val mainEndAtEpochMillis: Long,
    val napStartAtEpochMillis: Long?,
    val napEndAtEpochMillis: Long?
)

object NapRecoveryPlanner {
    private val targetWorkStarts = setOf(
        LocalTime.of(0, 30),
        LocalTime.of(2, 30),
        LocalTime.of(4, 30)
    )

    private val offDutyBuffer = Duration.ofMinutes(30)
    private val preWorkBuffer = Duration.ofMinutes(60)
    private val mainSleepTarget = Duration.ofHours(5)
    private val napGap = Duration.ofMinutes(30)
    private val napMin = Duration.ofMinutes(45)
    private val napMax = Duration.ofMinutes(90)

    fun nextSchedule(
        nowEpochMillis: Long,
        futureWorkEvents: List<ShiftEvent>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): NapRecoverySchedule? {
        if (futureWorkEvents.size < 2) return null
        val sorted = futureWorkEvents.sortedBy { it.startAtEpochMillis }

        for (index in 0 until sorted.lastIndex) {
            val current = sorted[index]
            val next = sorted[index + 1]
            if (!isTargetNightShift(current, zoneId)) continue

            val windowStart = current.endAtEpochMillis + offDutyBuffer.toMillis()
            val windowEnd = next.startAtEpochMillis - preWorkBuffer.toMillis()
            if (windowEnd <= windowStart) continue

            val mainStart = windowStart
            val latestMainEnd = windowEnd - napMin.toMillis()
            val preferredMainEnd = mainStart + mainSleepTarget.toMillis()
            val mainEnd = minOf(preferredMainEnd, latestMainEnd)
            if (mainEnd <= mainStart) continue

            var napStart: Long? = null
            var napEnd: Long? = null
            val proposedNapStart = mainEnd + napGap.toMillis()
            if (proposedNapStart < windowEnd) {
                val available = windowEnd - proposedNapStart
                val napDuration = minOf(available, napMax.toMillis())
                if (napDuration >= napMin.toMillis()) {
                    napStart = proposedNapStart
                    napEnd = proposedNapStart + napDuration
                }
            }

            val scheduleEnd = napEnd ?: mainEnd
            if (scheduleEnd <= nowEpochMillis) continue

            return NapRecoverySchedule(
                mainStartAtEpochMillis = mainStart,
                mainEndAtEpochMillis = mainEnd,
                napStartAtEpochMillis = napStart,
                napEndAtEpochMillis = napEnd
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
}
