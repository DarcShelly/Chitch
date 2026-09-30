package com.chitch.app.di

import android.content.Context
import com.chitch.app.data.AppDatabase
import com.chitch.app.data.OnboardingPreferences
import com.chitch.app.data.repository.DailyHabitCheckRepository
import com.chitch.app.data.repository.DailyReviewRepository
import com.chitch.app.data.repository.HabitRepository
import com.chitch.app.data.repository.RoutineRepository
import com.chitch.app.data.repository.TaskLedgerRepository
import com.chitch.app.data.repository.TaskRepository
import com.chitch.app.data.repository.UserRepository
import com.chitch.app.notification.NotificationScheduler
import com.chitch.app.widget.WidgetRefresher

/**
 * Hand-rolled service locator. A habit tracker this size doesn't need Hilt —
 * one container with a handful of repositories is simpler to read and maintain.
 */
class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val notificationScheduler = NotificationScheduler(context)
    private val widgetRefresher = WidgetRefresher(context)

    val taskLedgerRepository = TaskLedgerRepository(database.taskLedgerDao())
    val taskRepository = TaskRepository(database.taskDao(), taskLedgerRepository, notificationScheduler, widgetRefresher)
    val routineRepository = RoutineRepository(database, taskLedgerRepository, notificationScheduler, widgetRefresher)
    val habitRepository = HabitRepository(database, taskLedgerRepository, notificationScheduler, widgetRefresher)
    val userRepository = UserRepository(database.userDao())
    val dailyReviewRepository = DailyReviewRepository(database, habitRepository, userRepository, widgetRefresher)
    val dailyHabitCheckRepository = DailyHabitCheckRepository(database.dailyHabitCheckDao())
    val onboardingPreferences = OnboardingPreferences(context)
}
