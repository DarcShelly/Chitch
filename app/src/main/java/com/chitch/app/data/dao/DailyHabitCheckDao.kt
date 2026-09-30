package com.chitch.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chitch.app.data.entity.DailyHabitCheckEntity
import java.time.LocalDate

@Dao
interface DailyHabitCheckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(checks: List<DailyHabitCheckEntity>)

    @Query("DELETE FROM daily_habit_checks WHERE date = :date")
    suspend fun deleteForDate(date: LocalDate)

    @Query("DELETE FROM daily_habit_checks WHERE date = :date AND habitId = :habitId")
    suspend fun deleteForDateAndHabit(date: LocalDate, habitId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM daily_habit_checks WHERE date = :date AND habitId = :habitId)")
    suspend fun isChecked(date: LocalDate, habitId: Long): Boolean

    @Query("SELECT habitId FROM daily_habit_checks WHERE date = :date")
    suspend fun getCheckedHabitIds(date: LocalDate): List<Long>

    @Query("SELECT date FROM daily_habit_checks WHERE habitId = :habitId AND date BETWEEN :start AND :end")
    suspend fun getDoneDates(habitId: Long, start: LocalDate, end: LocalDate): List<LocalDate>

    @Query("DELETE FROM daily_habit_checks WHERE date < :cutoff")
    suspend fun deleteOlderThan(cutoff: LocalDate)
}
