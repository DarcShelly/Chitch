package com.chitch.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitch.app.data.entity.HabitEntity
import com.chitch.app.data.entity.RoutineEntity
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.entity.TaskStatus
import com.chitch.app.data.repository.HabitRepository
import com.chitch.app.data.repository.RoutineRepository
import com.chitch.app.data.repository.TaskRepository
import com.chitch.app.domain.isCompletedNow
import com.chitch.app.domain.sortedForDisplay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

data class HomeHabitItem(val habit: HabitEntity, val taskCount: Int, val nextTask: TaskEntity?)
data class HomeRoutineItem(val routine: RoutineEntity, val taskCount: Int, val nextTask: TaskEntity?)
data class HomeTaskItem(val task: TaskEntity, val parentLabel: String?)

data class HomeUiState(
    val habitItems: List<HomeHabitItem> = emptyList(),
    val routineItem: HomeRoutineItem? = null,
    val taskItems: List<HomeTaskItem> = emptyList()
)

class HomeViewModel(
    routineRepository: RoutineRepository,
    private val taskRepository: TaskRepository,
    habitRepository: HabitRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        habitRepository.observeActive(),
        routineRepository.observeActive(),
        taskRepository.observeAllTasksRaw()
    ) { habits, routine, allTasks ->
        val activeTasks = allTasks.filter { it.status == TaskStatus.ACTIVE }

        fun nextTaskOf(tasks: List<TaskEntity>) = tasks
            .filterNot { it.isCompletedNow() }
            .sortedBy { it.startTime ?: it.dueTime ?: LocalTime.MAX }
            .firstOrNull()

        val habitItems = habits.map { habit ->
            val habitTasks = activeTasks.filter { it.habitId == habit.id }
            HomeHabitItem(habit, habitTasks.size, nextTaskOf(habitTasks))
        }

        val routineItem = routine?.let { r ->
            val routineTasks = activeTasks.filter { it.routineId == r.id }
            HomeRoutineItem(r, routineTasks.size, nextTaskOf(routineTasks))
        }

        val habitTitleById = habits.associate { it.id to it.title }
        val taskItems = activeTasks.sortedForDisplay().map { task ->
            val parentLabel = when {
                task.habitId != null -> habitTitleById[task.habitId]
                task.routineId != null && routine != null && task.routineId == routine.id -> routine.title
                else -> null
            }
            HomeTaskItem(task, parentLabel)
        }

        HomeUiState(habitItems, routineItem, taskItems)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch {
            taskRepository.evaluateAndRecordMisses()
            taskRepository.resyncNotifications()
        }
    }

    fun completeTask(taskId: Long) {
        viewModelScope.launch { taskRepository.completeTask(taskId) }
    }

    fun uncompleteTask(taskId: Long) {
        viewModelScope.launch { taskRepository.uncompleteTask(taskId) }
    }
}
