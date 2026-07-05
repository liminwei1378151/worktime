package com.example.worktime.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.worktime.data.local.entity.ShiftPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftPlanDao {

    @Query("SELECT * FROM shift_plan WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ShiftPlanEntity?>

    @Query("SELECT * FROM shift_plan WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ShiftPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ShiftPlanEntity)
}
