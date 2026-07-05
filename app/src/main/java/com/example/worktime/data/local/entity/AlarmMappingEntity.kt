package com.example.worktime.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alarm_mapping",
    foreignKeys = [
        ForeignKey(
            entity = ShiftPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShiftEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId"), Index(value = ["requestCode", "alarmType"], unique = true), Index("eventId")]
)
data class AlarmMappingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val planId: Long,
    val eventId: Long,
    val requestCode: Int,
    val alarmType: String,
    val workStartAtEpochMillis: Long,
    val triggerAtEpochMillis: Long,
    val createdAtEpochMillis: Long
)