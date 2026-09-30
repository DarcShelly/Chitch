package com.chitch.app.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitch.app.data.entity.HabitEntity
import com.chitch.app.data.entity.HabitStatus
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.entity.TaskStatus
import com.chitch.app.data.repository.DailyHabitCheckRepository
import com.chitch.app.data.repository.HabitRepository
import com.chitch.app.data.repository.TaskRepository
import com.chitch.app.domain.StreakCalculator
import com.chitch.app.domain.isCompletedNow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class HabitListItem(
    val habit: HabitEntity,
    val taskCount: Int,
    val nextTask: TaskEntity?,
    val currentStreak: Int
)

class HabitsListViewModel(
    private val habitRepository: HabitRepository,
    taskRepository: TaskRepository,
    private val dailyHabitCheckRepository: DailyHabitCheckRepository
) : ViewModel() {

    val items: StateFlow<List<HabitListItem>> = combine(
        habitRepository.observeAll(),
        taskRepository.observeAllTasksRaw()
    ) { habits, allTasks ->
        val today = LocalDate.now()
        habits.map { habit ->
            val habitTasks = allTasks.filter { it.habitId == habit.id && it.status == TaskStatus.ACTIVE }
            val nextTask = habitTasks
                .filterNot { it.isCompletedNow() }
                .sortedBy { it.startTime ?: it.dueTime ?: LocalTime.MAX }
                .firstOrNull()
            val doneDates = dailyHabitCheckRepository.getDoneDates(habit.id, habit.streakWindowStartDate, today)
            val streak = StreakCalculator.currentStreak(doneDates, today)
            HabitListItem(habit, habitTasks.size, nextTask, streak)
        }.sortedWith(
            compareBy(
                { it.habit.status != HabitStatus.ACTIVE },
                { it.nextTask?.let { t -> t.startTime ?: t.dueTime } ?: LocalTime.MAX }
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _reactivateError = MutableStateFlow<String?>(null)
    val reactivateError: StateFlow<String?> = _reactivateError.asStateFlow()

    fun reactivate(habitId: Long) {
        viewModelScope.launch {
            val activated = habitRepository.activateHabit(habitId)
            if (!activated) _reactivateError.value = "3 habits are already active — archive or complete one first."
        }
    }

    fun dismissReactivateError() {
        _reactivateError.value = null
    }
}
