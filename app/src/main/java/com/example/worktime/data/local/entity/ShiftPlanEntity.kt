package com.example.worktime.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shift_plan")
data class ShiftPlanEntity(
    @PrimaryKey val id: Long,
    val totalDays: Int,
    val firstWorkStartAtEpochMillis: Long,
    val silentRemindBeforeMinutes: Int,
    val departRemindBeforeMinutes: Int,
    val updatedAtEpochMillis: Long
)