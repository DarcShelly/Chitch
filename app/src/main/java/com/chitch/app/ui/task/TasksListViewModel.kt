package com.chitch.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.repository.HabitRepository
import com.chitch.app.data.repository.RoutineRepository
import com.chitch.app.data.repository.TaskRepository
import com.chitch.app.domain.isCompletedNow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TaskListItem(val task: TaskEntity, val parentLabel: String?)

/** A completion/archive that just happened and can still be undone from the snackbar. */
data class UndoEvent(val snapshot: TaskEntity, val wasNewCompletion: Boolean, val actionLabel: String)

class TasksListViewModel(
    private val taskRepository: TaskRepository,
    habitRepository: HabitRepository,
    routineRepository: RoutineRepository
) : ViewModel() {

    /** Every task regardless of container — a task attached to a habit/routine still shows here. */
    val tasks: StateFlow<List<TaskListItem>> = combine(
        taskRepository.observeAllTasksRaw(),
        habitRepository.observeAll(),
        routineRepository.observeAll()
    ) { allTasks, habits, routines ->
        val habitTitleById = habits.associate { it.id to it.title }
        val routineTitleById = routines.associate { it.id to it.title }
        allTasks.map { task ->
            val parentLabel = when {
                task.habitId != null -> habitTitleById[task.habitId]
                task.routineId != null -> routineTitleById[task.routineId]
                else -> null
            }
            TaskListItem(task, parentLabel)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _undoEvent = MutableStateFlow<UndoEvent?>(null)
    val undoEvent: StateFlow<UndoEvent?> = _undoEvent.asStateFlow()

    fun completeTask(task: TaskEntity) {
        val wasNewCompletion = !task.isCompletedNow()
        viewModelScope.launch {
            taskRepository.completeTask(task.id)
            _undoEvent.value = UndoEvent(task, wasNewCompletion, "Task completed")
        }
    }

    fun uncompleteTask(taskId: Long) {
        viewModelScope.launch { taskRepository.uncompleteTask(taskId) }
    }

    fun archiveTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.archiveTask(task.id)
            _undoEvent.value = UndoEvent(task, wasNewCompletion = false, actionLabel = "Task archived")
        }
    }

    fun undo(event: UndoEvent) {
        viewModelScope.launch { taskRepository.undoAction(event.snapshot, event.wasNewCompletion) }
    }

    fun dismissUndoEvent() {
        _undoEvent.value = null
    }
}
