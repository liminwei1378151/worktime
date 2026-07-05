package com.example.worktime.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.worktime.data.local.entity.PlanHistoryEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanHistoryEventDao {

    @Insert
    suspend fun insertAll(events: List<PlanHistoryEventEntity>)

    @Query("SELECT * FROM plan_history_event WHERE historyId = :historyId ORDER BY startAtEpochMillis ASC")
    fun observeByHistoryId(historyId: Long): Flow<List<PlanHistoryEventEntity>>

    @Query("SELECT * FROM plan_history_event ORDER BY startAtEpochMillis ASC")
    fun observeAll(): Flow<List<PlanHistoryEventEntity>>

    @Query("SELECT * FROM plan_history_event WHERE historyId = :historyId ORDER BY startAtEpochMillis ASC")
    suspend fun getByHistoryId(historyId: Long): List<PlanHistoryEventEntity>

    @Query("SELECT * FROM plan_history_event ORDER BY startAtEpochMillis ASC")
    suspend fun getAll(): List<PlanHistoryEventEntity>

    @Query(
        "UPDATE plan_history_event SET startAtEpochMillis = :startAtEpochMillis, " +
            "endAtEpochMillis = :endAtEpochMillis WHERE id = :id"
    )
    suspend fun updateEventTimes(id: Long, startAtEpochMillis: Long, endAtEpochMillis: Long)

    @Query("DELETE FROM plan_history_event WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM plan_history_event WHERE startAtEpochMillis >= :fromEpochMillis AND startAtEpochMillis < :toEpochMillis")
    suspend fun deleteByEpochMillisRange(fromEpochMillis: Long, toEpochMillis: Long)
}
