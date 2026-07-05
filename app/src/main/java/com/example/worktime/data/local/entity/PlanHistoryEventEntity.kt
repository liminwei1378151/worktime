package com.example.worktime.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "plan_history_event",
    foreignKeys = [
        ForeignKey(
            entity = PlanHistoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["historyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("historyId")]
)
data class PlanHistoryEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val historyId: Long,
    val type: String,
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long,
    val alarmTriggerAtEpochMillis: Long?,
    val silentAlarmTriggerAtEpochMillis: Long?,
    val departAlarmTriggerAtEpochMillis: Long?
)
