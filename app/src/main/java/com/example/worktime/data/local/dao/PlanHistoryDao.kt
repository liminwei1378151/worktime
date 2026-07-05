package com.example.worktime.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.worktime.data.local.entity.PlanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanHistoryDao {

    @Insert
    suspend fun insert(entity: PlanHistoryEntity): Long

    @Query("SELECT * FROM plan_history ORDER BY savedAtEpochMillis DESC")
    fun observeAll(): Flow<List<PlanHistoryEntity>>

    @Query("SELECT * FROM plan_history ORDER BY savedAtEpochMillis DESC")
    suspend fun getAll(): List<PlanHistoryEntity>

    @Query("DELETE FROM plan_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
