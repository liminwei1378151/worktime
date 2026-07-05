package com.example.worktime.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.worktime.data.local.entity.ShiftEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftEventDao {

    @Query("SELECT * FROM shift_event WHERE planId = :planId ORDER BY startAtEpochMillis ASC")
    fun observeByPlanId(planId: Long): Flow<List<ShiftEventEntity>>

    @Query("SELECT * FROM shift_event WHERE planId = :planId ORDER BY startAtEpochMillis ASC")
    suspend fun getByPlanId(planId: Long): List<ShiftEventEntity>

    @Query(
        "SELECT * FROM shift_event WHERE planId = :planId " +
            "AND type = 'WORK' AND startAtEpochMillis >= :fromEpochMillis " +
            "ORDER BY startAtEpochMillis ASC"
    )
    suspend fun getFutureWorkEvents(
        planId: Long,
        fromEpochMillis: Long
    ): List<ShiftEventEntity>

    @Insert
    suspend fun insertAll(entities: List<ShiftEventEntity>)

    @Query("DELETE FROM shift_event WHERE planId = :planId")
    suspend fun deleteByPlanId(planId: Long)
}
