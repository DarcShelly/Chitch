package com.chitch.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chitch.app.data.entity.RoutineEntity
import com.chitch.app.data.entity.RoutineStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Insert
    suspend fun insert(routine: RoutineEntity): Long

    @Update
    suspend fun update(routine: RoutineEntity)

    @Delete
    suspend fun delete(routine: RoutineEntity)

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getById(id: Long): RoutineEntity?

    @Query("SELECT * FROM routines ORDER BY created_at DESC")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE status = 'ACTIVE' LIMIT 1")
    fun observeActive(): Flow<RoutineEntity?>

    @Query("SELECT * FROM routines WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun getActive(): RoutineEntity?

    @Query("UPDATE routines SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: RoutineStatus)
}
