package com.chitch.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

/**
 * Minimal subset of the future User entity — only the fields routine-hour
 * validation depends on. Singleton row, always id = 1. See clarification #7.
 */
@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,

    val workStart: LocalTime? = null,
    val workEnd: LocalTime? = null,

    /** Minimum 6, defaults to 8. Enforced in the repository layer, not the DB. */
    val sleepHours: Int = DEFAULT_SLEEP_HOURS,

    /** Misses allowed within the rolling 30-day window before a habit stops being "completable". */
    val maxMissedDaysForHabitCompletion: Int = DEFAULT_MAX_MISSED_DAYS
) {
    companion object {
        const val SINGLETON_ID = 1
        const val MIN_SLEEP_HOURS = 6
        const val DEFAULT_SLEEP_HOURS = 8
        const val MIN_MAX_MISSED_DAYS = 0
        const val DEFAULT_MAX_MISSED_DAYS = 3
    }
}
