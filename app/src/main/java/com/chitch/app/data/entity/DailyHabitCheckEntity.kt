package com.chitch.app.data.entity

import androidx.room.Entity
import java.time.LocalDate

/**
 * Records that a habit was checked off as "done" on a given day, via the daily review's habit
 * checklist. Absence of a row for a (date, habitId) pair means it was missed that day — see
 * [com.chitch.app.domain.HabitCompletionEvaluator].
 */
@Entity(tableName = "daily_habit_checks", primaryKeys = ["date", "habitId"])
data class DailyHabitCheckEntity(
    val date: LocalDate,
    val habitId: Long
)
