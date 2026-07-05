package com.example.worktime.domain.model

import com.example.worktime.data.local.entity.PlanHistoryEventEntity

data class PlanHistoryEvent(
    val id: Long,
    val historyId: Long,
    val type: ShiftEventType,
    val startAtEpochMillis: Long,
    val endAtEpochMillis: Long,
    val alarmTriggerAtEpochMillis: Long?,
    val silentAlarmTriggerAtEpochMillis: Long?,
    val departAlarmTriggerAtEpochMillis: Long?
)

internal fun PlanHistoryEventEntity.toDomain(): PlanHistoryEvent {
    return PlanHistoryEvent(
        id = id,
        historyId = historyId,
        type = ShiftEventType.valueOf(type),
        startAtEpochMillis = startAtEpochMillis,
        endAtEpochMillis = endAtEpochMillis,
        alarmTriggerAtEpochMillis = alarmTriggerAtEpochMillis,
        silentAlarmTriggerAtEpochMillis = silentAlarmTriggerAtEpochMillis,
        departAlarmTriggerAtEpochMillis = departAlarmTriggerAtEpochMillis
    )
}

internal fun PlanHistoryEvent.toEntity(): PlanHistoryEventEntity {
    return PlanHistoryEventEntity(
        historyId = historyId,
        type = type.name,
        startAtEpochMillis = startAtEpochMillis,
        endAtEpochMillis = endAtEpochMillis,
        alarmTriggerAtEpochMillis = alarmTriggerAtEpochMillis,
        silentAlarmTriggerAtEpochMillis = silentAlarmTriggerAtEpochMillis,
        departAlarmTriggerAtEpochMillis = departAlarmTriggerAtEpochMillis
    )
}
