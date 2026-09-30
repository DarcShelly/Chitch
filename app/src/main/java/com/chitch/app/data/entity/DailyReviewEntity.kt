package com.chitch.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.time.Instant
import java.time.LocalDate

/**
 * One row per calendar day. Task/habit counts are snapshotted at submission time (rather than
 * re-derived later from task history, which the app doesn't keep a historical log of) so that
 * week/month summaries are a simple aggregate over these rows. Purged beyond 90 days —
 * see [com.chitch.app.data.repository.DailyReviewRepository.purgeOldData].
 */
@Entity(tableName = "daily_reviews")
data class DailyReviewEntity(
    @androidx.room.PrimaryKey
    val date: LocalDate,

    val mood: MoodRating,
    val notesForTomorrow: String? = null,
    val thingsLiked: String? = null,
    val thingsMissed: String? = null,

    val tasksDoneCount: Int = 0,
    val tasksMissedCount: Int = 0,
    val habitsCheckedCount: Int = 0,
    val habitsMissedCount: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
