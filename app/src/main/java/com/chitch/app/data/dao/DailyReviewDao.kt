package com.chitch.app.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.chitch.app.data.entity.DailyReviewEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyReviewDao {
    @Upsert
    suspend fun upsert(review: DailyReviewEntity)

    @Query("SELECT * FROM daily_reviews WHERE date = :date")
    suspend fun getByDate(date: LocalDate): DailyReviewEntity?

    @Query("SELECT * FROM daily_reviews WHERE date = :date")
    fun observeByDate(date: LocalDate): Flow<DailyReviewEntity?>

    @Query("SELECT * FROM daily_reviews WHERE date BETWEEN :start AND :end ORDER BY date")
    suspend fun getBetween(start: LocalDate, end: LocalDate): List<DailyReviewEntity>

    @Query("DELETE FROM daily_reviews WHERE date < :cutoff")
    suspend fun deleteOlderThan(cutoff: LocalDate)
}
