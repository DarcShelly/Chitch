package com.chitch.app.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.chitch.app.ui.dailyreview.DailyReviewViewModel
import com.chitch.app.ui.habit.CreateHabitViewModel
import com.chitch.app.ui.habit.HabitsListViewModel
import com.chitch.app.ui.home.HomeViewModel
import com.chitch.app.ui.routine.CreateRoutineViewModel
import com.chitch.app.ui.routine.RoutinesListViewModel
import com.chitch.app.ui.settings.SettingsViewModel
import com.chitch.app.ui.stats.StatsViewModel
import com.chitch.app.ui.summary.SummaryViewModel
import com.chitch.app.ui.task.CreateTaskViewModel
import com.chitch.app.ui.task.TasksListViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(container.routineRepository, container.taskRepository, container.habitRepository) as T

        modelClass.isAssignableFrom(CreateTaskViewModel::class.java) ->
            CreateTaskViewModel(container.taskRepository) as T

        modelClass.isAssignableFrom(CreateRoutineViewModel::class.java) ->
            CreateRoutineViewModel(container.routineRepository, container.userRepository) as T

        modelClass.isAssignableFrom(CreateHabitViewModel::class.java) ->
            CreateHabitViewModel(container.habitRepository) as T

        modelClass.isAssignableFrom(TasksListViewModel::class.java) ->
            TasksListViewModel(container.taskRepository, container.habitRepository, container.routineRepository) as T

        modelClass.isAssignableFrom(RoutinesListViewModel::class.java) ->
            RoutinesListViewModel(container.routineRepository, container.taskRepository) as T

        modelClass.isAssignableFrom(HabitsListViewModel::class.java) ->
            HabitsListViewModel(container.habitRepository, container.taskRepository, container.dailyHabitCheckRepository) as T

        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(container.userRepository) as T

        modelClass.isAssignableFrom(StatsViewModel::class.java) ->
            StatsViewModel(container.taskLedgerRepository, container.habitRepository, container.dailyHabitCheckRepository) as T

        modelClass.isAssignableFrom(DailyReviewViewModel::class.java) ->
            DailyReviewViewModel(container.dailyReviewRepository, container.taskRepository, container.habitRepository) as T

        modelClass.isAssignableFrom(SummaryViewModel::class.java) ->
            SummaryViewModel(container.dailyReviewRepository) as T

        else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
