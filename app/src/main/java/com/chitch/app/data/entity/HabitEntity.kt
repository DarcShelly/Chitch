package com.chitch.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val description: String? = null,
    val status: HabitStatus = HabitStatus.DRAFT,
    val imageUri: String? = null,
    val fileUris: List<String> = emptyList(),

    /**
     * Start of the current 30-day streak-tracking window (see [com.chitch.app.domain.HabitCompletionEvaluator]).
     * Reset to today whenever the habit is (re)activated, so a reactivated habit starts a fresh window.
     */
    val streakWindowStartDate: LocalDate,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
