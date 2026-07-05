package com.example.worktime.domain.model

enum class ShiftEventType {
    WORK,
    REST
}

data class ShiftEvent(
    val id: Long = 0L,
    val planId: Long,
    val type: ShiftEventType,
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long,
    val alarmTriggerAtEpochMillis: Long? = null,
    val silentAlarmTriggerAtEpochMillis: Long? = null,
    val departAlarmTriggerAtEpochMillis: Long? = null
)
