package com.example.worktime.domain.model

import java.time.LocalTime

data class SleepSlot(
    val startTime: LocalTime,
    val endTime: LocalTime,
    val type: SleepType,
    val isPreviousDay: Boolean
) {
    val durationMinutes: Int
        get() {
            val startMinutes = startTime.hour * 60 + startTime.minute
            val endMinutes = endTime.hour * 60 + endTime.minute
            return if (endMinutes >= startMinutes) endMinutes - startMinutes else (24 * 60 - startMinutes) + endMinutes
        }

    val durationHoursString: String
        get() {
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            return if (minutes > 0) "${hours}.${minutes * 10 / 60}" else "$hours"
        }
}

enum class SleepType {
    MAIN,
    SUPPLEMENT
}
