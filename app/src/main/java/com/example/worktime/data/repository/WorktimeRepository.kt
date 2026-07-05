package com.example.worktime.data.repository

import androidx.room.withTransaction
import com.example.worktime.data.local.db.WorktimeDatabase
import com.example.worktime.data.local.entity.AlarmMappingEntity
import com.example.worktime.data.local.entity.ShiftEventEntity
import com.example.worktime.data.local.entity.ShiftPlanEntity
import com.example.worktime.domain.model.AlarmMapping
import com.example.worktime.domain.model.AlarmType
import com.example.worktime.domain.model.PlanHistory
import com.example.worktime.domain.model.PlanHistoryEvent
import com.example.worktime.domain.model.ShiftEvent
import com.example.worktime.domain.model.ShiftEventType
import com.example.worktime.domain.model.ShiftPlan
import com.example.worktime.domain.model.toDomain
import com.example.worktime.domain.model.toEntity
import com.example.worktime.domain.scheduler.ShiftScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WorktimeRepository(
    private val database: WorktimeDatabase,
    private val shiftScheduler: ShiftScheduler
) {
    private val planDao = database.shiftPlanDao()
    private val eventDao = database.shiftEventDao()
    private val alarmDao = database.alarmMappingDao()
    private val historyDao = database.planHistoryDao()
    private val historyEventDao = database.planHistoryEventDao()

    fun observePlan(): Flow<ShiftPlan?> {
        return planDao.observeById(ShiftPlan.SINGLE_PLAN_ID).map { it?.toDomain() }
    }

    fun observeEvents(): Flow<List<ShiftEvent>> {
        return eventDao.observeByPlanId(ShiftPlan.SINGLE_PLAN_ID).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun observeAlarmMappings(): Flow<List<AlarmMapping>> {
        return alarmDao.observeByPlanId(ShiftPlan.SINGLE_PLAN_ID).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getPlan(): ShiftPlan? {
        return planDao.getById(ShiftPlan.SINGLE_PLAN_ID)?.toDomain()
    }

    suspend fun savePlanAndRegenerateEvents(plan: ShiftPlan) {
        database.withTransaction {
            val currentPlanEntity = planDao.getById(ShiftPlan.SINGLE_PLAN_ID)
            if (currentPlanEntity != null) {
                val currentEvents = eventDao.getByPlanId(ShiftPlan.SINGLE_PLAN_ID)
                val historyEntity = currentPlanEntity.toHistoryEntity()
                val historyId = historyDao.insert(historyEntity)
                if (currentEvents.isNotEmpty()) {
                    historyEventDao.insertAll(currentEvents.map { it.toHistoryEntity(historyId) })
                }
            }

            val generatedEvents = shiftScheduler.generate(plan)
            planDao.upsert(plan.toEntity())
            eventDao.deleteByPlanId(plan.id)
            if (generatedEvents.isNotEmpty()) {
                eventDao.insertAll(generatedEvents.map { it.toEntity() })
            }
        }
    }

    suspend fun getFutureWorkEvents(fromEpochMillis: Long): List<ShiftEvent> {
        return eventDao.getFutureWorkEvents(
            planId = ShiftPlan.SINGLE_PLAN_ID,
            fromEpochMillis = fromEpochMillis
        ).map { it.toDomain() }
    }

    suspend fun replaceAlarmMappings(mappings: List<AlarmMapping>) {
        database.withTransaction {
            alarmDao.deleteByPlanId(ShiftPlan.SINGLE_PLAN_ID)
            if (mappings.isNotEmpty()) {
                alarmDao.insertAll(mappings.map { it.toEntity() })
            }
        }
    }

    suspend fun getAlarmMappings(): List<AlarmMapping> {
        return alarmDao.getByPlanId(ShiftPlan.SINGLE_PLAN_ID).map { it.toDomain() }
    }

    fun observeAllHistoryEvents(): Flow<List<PlanHistoryEvent>> {
        return historyEventDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun updateHistoryEvent(id: Long, startAtEpochMillis: Long, endAtEpochMillis: Long) {
        historyEventDao.updateEventTimes(id, startAtEpochMillis, endAtEpochMillis)
    }

    suspend fun deleteHistoryEvent(id: Long) {
        historyEventDao.deleteById(id)
    }

    suspend fun deleteHistoryEventsByDate(date: java.time.LocalDate) {
        val zone = java.time.ZoneId.systemDefault()
        val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        historyEventDao.deleteByEpochMillisRange(from, to)
    }
}

private fun ShiftPlanEntity.toDomain(): ShiftPlan {
    return ShiftPlan(
        id = id,
        totalDays = totalDays,
        firstWorkStartAtEpochMillis = firstWorkStartAtEpochMillis,
        silentRemindBeforeMinutes = silentRemindBeforeMinutes,
        departRemindBeforeMinutes = departRemindBeforeMinutes,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}

private fun ShiftPlan.toEntity(): ShiftPlanEntity {
    return ShiftPlanEntity(
        id = id,
        totalDays = totalDays,
        firstWorkStartAtEpochMillis = firstWorkStartAtEpochMillis,
        silentRemindBeforeMinutes = silentRemindBeforeMinutes,
        departRemindBeforeMinutes = departRemindBeforeMinutes,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}

private fun ShiftPlanEntity.toHistoryEntity(): com.example.worktime.data.local.entity.PlanHistoryEntity {
    return com.example.worktime.data.local.entity.PlanHistoryEntity(
        totalDays = totalDays,
        firstWorkStartAtEpochMillis = firstWorkStartAtEpochMillis,
        silentRemindBeforeMinutes = silentRemindBeforeMinutes,
        departRemindBeforeMinutes = departRemindBeforeMinutes,
        updatedAtEpochMillis = updatedAtEpochMillis,
        savedAtEpochMillis = System.currentTimeMillis()
    )
}

private fun ShiftEventEntity.toHistoryEntity(historyId: Long): com.example.worktime.data.local.entity.PlanHistoryEventEntity {
    return com.example.worktime.data.local.entity.PlanHistoryEventEntity(
        historyId = historyId,
        type = type,
        startAtEpochMillis = startAtEpochMillis,
        endAtEpochMillis = endAtEpochMillis,
        alarmTriggerAtEpochMillis = alarmTriggerAtEpochMillis,
        silentAlarmTriggerAtEpochMillis = silentAlarmTriggerAtEpochMillis,
        departAlarmTriggerAtEpochMillis = departAlarmTriggerAtEpochMillis
    )
}

private fun ShiftEventEntity.toDomain(): ShiftEvent {
    return ShiftEvent(
        id = id,
        planId = planId,
        type = ShiftEventType.valueOf(type),
        startAtEpochMillis = startAtEpochMillis,
        endAtEpochMillis = endAtEpochMillis,
        alarmTriggerAtEpochMillis = alarmTriggerAtEpochMillis,
        silentAlarmTriggerAtEpochMillis = silentAlarmTriggerAtEpochMillis,
        departAlarmTriggerAtEpochMillis = departAlarmTriggerAtEpochMillis
    )
}

private fun ShiftEvent.toEntity(): ShiftEventEntity {
    return ShiftEventEntity(
        planId = planId,
        type = type.name,
        startAtEpochMillis = startAtEpochMillis,
        endAtEpochMillis = endAtEpochMillis,
        alarmTriggerAtEpochMillis = alarmTriggerAtEpochMillis,
        silentAlarmTriggerAtEpochMillis = silentAlarmTriggerAtEpochMillis,
        departAlarmTriggerAtEpochMillis = departAlarmTriggerAtEpochMillis
    )
}

private fun AlarmMappingEntity.toDomain(): AlarmMapping {
    return AlarmMapping(
        id = id,
        planId = planId,
        eventId = eventId,
        requestCode = requestCode,
        alarmType = AlarmType.valueOf(alarmType),
        workStartAtEpochMillis = workStartAtEpochMillis,
        triggerAtEpochMillis = triggerAtEpochMillis,
        createdAtEpochMillis = createdAtEpochMillis
    )
}

private fun AlarmMapping.toEntity(): AlarmMappingEntity {
    return AlarmMappingEntity(
        id = id,
        planId = planId,
        eventId = eventId,
        requestCode = requestCode,
        alarmType = alarmType.name,
        workStartAtEpochMillis = workStartAtEpochMillis,
        triggerAtEpochMillis = triggerAtEpochMillis,
        createdAtEpochMillis = createdAtEpochMillis
    )
}
