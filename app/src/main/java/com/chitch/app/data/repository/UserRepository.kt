package com.chitch.app.data.repository

import com.chitch.app.data.dao.UserDao
import com.chitch.app.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

class UserRepository(private val userDao: UserDao) {

    fun observe(): Flow<UserEntity?> = userDao.observe()

    suspend fun get(): UserEntity = userDao.get() ?: UserEntity()

    suspend fun save(
        workStart: LocalTime?,
        workEnd: LocalTime?,
        sleepHours: Int,
        maxMissedDaysForHabitCompletion: Int = UserEntity.DEFAULT_MAX_MISSED_DAYS
    ) {
        val clampedSleep = sleepHours.coerceAtLeast(UserEntity.MIN_SLEEP_HOURS)
        val clampedMisses = maxMissedDaysForHabitCompletion.coerceAtLeast(UserEntity.MIN_MAX_MISSED_DAYS)
        userDao.upsert(
            UserEntity(
                workStart = workStart,
                workEnd = workEnd,
                sleepHours = clampedSleep,
                maxMissedDaysForHabitCompletion = clampedMisses
            )
        )
    }
}
