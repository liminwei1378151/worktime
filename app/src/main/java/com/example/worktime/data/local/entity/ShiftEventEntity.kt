package com.example.worktime.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shift_event",
    foreignKeys = [
        ForeignKey(
            entity = ShiftPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId"), Index("startAtEpochMillis"), Index("type")]
)
data class ShiftEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val planId: Long,
    val type: String,
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long,
    val alarmTriggerAtEpochMillis: Long?,
    val silentAlarmTriggerAtEpochMillis: Long?,
    val departAlarmTriggerAtEpochMillis: Long?
)