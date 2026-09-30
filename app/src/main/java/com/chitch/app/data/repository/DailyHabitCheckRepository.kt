package com.chitch.app.data.repository

import com.chitch.app.data.dao.DailyHabitCheckDao
import com.chitch.app.data.entity.DailyHabitCheckEntity
import java.time.LocalDate

class DailyHabitCheckRepository(private val dao: DailyHabitCheckDao) {

    suspend fun getDoneDates(habitId: Long, start: LocalDate, end: LocalDate): Set<LocalDate> =
        dao.getDoneDates(habitId, start, end).toSet()

    /** Checks a habit off for [date] if it isn't already, un-checks it if it is. */
    suspend fun toggleCheck(habitId: Long, date: LocalDate) {
        if (dao.isChecked(date, habitId)) {
            dao.deleteForDateAndHabit(date, habitId)
        } else {
            dao.insertAll(listOf(DailyHabitCheckEntity(date = date, habitId = habitId)))
        }
    }
}
