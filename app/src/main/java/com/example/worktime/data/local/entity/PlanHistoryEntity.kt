package com.example.worktime.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_history")
data class PlanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val totalDays: Int,
    val firstWorkStartAtEpochMillis: Long,
    val silentRemindBeforeMinutes: Int,
    val departRemindBeforeMinutes: Int,
    val updatedAtEpochMillis: Long,
    val savedAtEpochMillis: Long
)
