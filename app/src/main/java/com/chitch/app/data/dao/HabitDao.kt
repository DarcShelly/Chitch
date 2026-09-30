package com.chitch.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chitch.app.data.entity.HabitEntity
import com.chitch.app.data.entity.HabitStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface HabitDao {
    @Insert
    suspend fun insert(habit: HabitEntity): Long

    @Update
    suspend fun update(habit: HabitEntity)

    @Delete
    suspend fun delete(habit: HabitEntity)

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: Long): HabitEntity?

    @Query("SELECT * FROM habits ORDER BY created_at DESC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE status = 'ACTIVE' ORDER BY created_at")
    fun observeActive(): Flow<List<HabitEntity>>

    /** Suspend (one-shot) counterpart to [observeActive] — used by the daily review orchestration. */
    @Query("SELECT * FROM habits WHERE status = 'ACTIVE' ORDER BY created_at")
    suspend fun getActive(): List<HabitEntity>

    @Query("SELECT COUNT(*) FROM habits WHERE status = 'ACTIVE' AND id != :excludingId")
    suspend fun getActiveCount(excludingId: Long = -1): Int

    @Query("UPDATE habits SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: HabitStatus)

    /** Restarts a habit's 30-day streak-tracking window — called whenever a habit is (re)activated. */
    @Query("UPDATE habits SET streakWindowStartDate = :date WHERE id = :id")
    suspend fun resetStreakWindow(id: Long, date: LocalDate)
}
