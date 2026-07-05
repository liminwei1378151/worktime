package com.example.worktime.domain.model

enum class AlarmType {
    SILENT,
    DEPART
}

data class AlarmMapping(
    val id: Long = 0L,
    val planId: Long,
    val eventId: Long,
    val requestCode: Int,
    val alarmType: AlarmType,
    val workStartAtEpochMillis: Long,
    val triggerAtEpochMillis: Long,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
)
