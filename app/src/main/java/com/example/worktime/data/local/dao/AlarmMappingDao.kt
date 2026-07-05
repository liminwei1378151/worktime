package com.example.worktime.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.worktime.data.local.entity.AlarmMappingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmMappingDao {

    @Query("SELECT * FROM alarm_mapping WHERE planId = :planId ORDER BY triggerAtEpochMillis ASC")
    fun observeByPlanId(planId: Long): Flow<List<AlarmMappingEntity>>

    @Query("SELECT * FROM alarm_mapping WHERE planId = :planId ORDER BY triggerAtEpochMillis ASC")
    suspend fun getByPlanId(planId: Long): List<AlarmMappingEntity>

    @Insert
    suspend fun insertAll(entities: List<AlarmMappingEntity>)

    @Query("DELETE FROM alarm_mapping WHERE planId = :planId")
    suspend fun deleteByPlanId(planId: Long)
}
