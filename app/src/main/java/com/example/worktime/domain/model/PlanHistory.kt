package com.example.worktime.domain.model

import com.example.worktime.data.local.entity.PlanHistoryEntity

data class PlanHistory(
    val id: Long,
    val totalDays: Int,
    val firstWorkStartAtEpochMillis: Long,
    val silentRemindBeforeMinutes: Int,
    val departRemindBeforeMinutes: Int,
    val updatedAtEpochMillis: Long,
    val savedAtEpochMillis: Long
)

internal fun PlanHistoryEntity.toDomain(): PlanHistory {
    return PlanHistory(
        id = id,
        totalDays = totalDays,
        firstWorkStartAtEpochMillis = firstWorkStartAtEpochMillis,
        silentRemindBeforeMinutes = silentRemindBeforeMinutes,
        departRemindBeforeMinutes = departRemindBeforeMinutes,
        updatedAtEpochMillis = updatedAtEpochMillis,
        savedAtEpochMillis = savedAtEpochMillis
    )
}

internal fun PlanHistory.toEntity(): PlanHistoryEntity {
    return PlanHistoryEntity(
        id = id,
        totalDays = totalDays,
        firstWorkStartAtEpochMillis = firstWorkStartAtEpochMillis,
        silentRemindBeforeMinutes = silentRemindBeforeMinutes,
        departRemindBeforeMinutes = departRemindBeforeMinutes,
        updatedAtEpochMillis = updatedAtEpochMillis,
        savedAtEpochMillis = savedAtEpochMillis
    )
}
