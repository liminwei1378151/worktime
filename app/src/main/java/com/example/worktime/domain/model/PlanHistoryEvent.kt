package com.example.worktime.domain.model

import com.example.worktime.data.local.entity.PlanHistoryEventEntity

data class PlanHistoryEvent(
    val id: Long,
    val historyId: Long,
    val type: ShiftEventType,
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long
)

internal fun PlanHistoryEventEntity.toDomain(): PlanHistoryEvent {
    return PlanHistoryEvent(
        id = id,
        historyId = historyId,
        type = ShiftEventType.valueOf(type),
        startAtEpochMillis = startAtEpochMillis,
        endAtEpochMillis = endAtEpochMillis
    )
}

internal fun PlanHistoryEvent.toEntity(): PlanHistoryEventEntity {
    return PlanHistoryEventEntity(
        historyId = historyId,
        type = type.name,
        startAtEpochMillis = startAtEpochMillis,
        endAtEpochMillis = endAtEpochMillis,
        alarmTriggerAtEpochMillis = null,
        silentAlarmTriggerAtEpochMillis = null,
        departAlarmTriggerAtEpochMillis = null
    )
}
