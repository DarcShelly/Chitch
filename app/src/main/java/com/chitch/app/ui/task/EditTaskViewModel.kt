package com.chitch.app.ui.task

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitch.app.data.entity.HabitEntity
import com.chitch.app.data.entity.RepeatType
import com.chitch.app.data.entity.RoutineEntity
import com.chitch.app.data.entity.TaskStatus
import com.chitch.app.data.repository.HabitRepository
import com.chitch.app.data.repository.RoutineRepository
import com.chitch.app.data.repository.TaskAssignResult
import com.chitch.app.data.repository.TaskRepository
import com.chitch.app.data.repository.TaskUpdateResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class EditTaskUiState(
    val title: String = "",
    val description: String = "",
    val imageUri: String? = null,
    val fileUris: List<String> = emptyList(),
    val startTime: LocalTime? = null,
    val dueTime: LocalTime? = null,
    val repeatType: RepeatType = RepeatType.ONE_TIME,
    val selectedDaysOfWeek: Set<DayOfWeek> = emptySet(),
    val scheduledDate: LocalDate? = null,
    val notify: Boolean = true,
    val routineId: Long? = null,
    val habitId: Long? = null,
    val status: TaskStatus = TaskStatus.ACTIVE,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val notFound: Boolean = false
)

class EditTaskViewModel(
    private val taskId: Long,
    private val taskRepository: TaskRepository,
    private val routineRepository: RoutineRepository,
    private val habitRepository: HabitRepository
) : ViewModel() {

    val routines: StateFlow<List<RoutineEntity>> = routineRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val habits: StateFlow<List<HabitEntity>> = habitRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var uiState by mutableStateOf(EditTaskUiState())
        private set

    private var originalRoutineId: Long? = null
    private var originalHabitId: Long? = null

    init {
        viewModelScope.launch {
            val task = taskRepository.getById(taskId)
            uiState = if (task != null) {
                originalRoutineId = task.routineId
                originalHabitId = task.habitId
                EditTaskUiState(
                    title = task.title,
                    description = task.description.orEmpty(),
                    imageUri = task.imageUri,
                    fileUris = task.fileUris,
                    startTime = task.startTime,
                    dueTime = task.dueTime,
                    repeatType = task.repeatType,
                    selectedDaysOfWeek = task.selectedDaysOfWeek,
                    scheduledDate = task.scheduledDate,
                    notify = task.notify,
                    routineId = task.routineId,
                    habitId = task.habitId,
                    status = task.status,
                    loading = false
                )
            } else {
                uiState.copy(loading = false, notFound = true)
            }
        }
    }

    fun updateTitle(value: String) { uiState = uiState.copy(title = value, error = null) }
    fun updateDescription(value: String) { uiState = uiState.copy(description = value) }
    fun updateImage(uri: String?) { uiState = uiState.copy(imageUri = uri) }
    fun addFiles(uris: List<String>) { uiState = uiState.copy(fileUris = uiState.fileUris + uris) }
    fun removeFile(uri: String) { uiState = uiState.copy(fileUris = uiState.fileUris.filterNot { it == uri }) }
    fun updateStartTime(value: LocalTime?) { uiState = uiState.copy(startTime = value, error = null) }
    fun updateDueTime(value: LocalTime?) { uiState = uiState.copy(dueTime = value, error = null) }
    fun updateRepeatType(value: RepeatType) {
        uiState = uiState.copy(
            repeatType = value,
            selectedDaysOfWeek = if (value == RepeatType.WEEKLY) uiState.selectedDaysOfWeek else emptySet()
        )
    }
    fun toggleDayOfWeek(day: DayOfWeek) {
        val current = uiState.selectedDaysOfWeek
        uiState = uiState.copy(selectedDaysOfWeek = if (day in current) current - day else current + day)
    }
    fun updateScheduledDate(value: LocalDate?) { uiState = uiState.copy(scheduledDate = value) }
    fun updateNotify(value: Boolean) { uiState = uiState.copy(notify = value) }
    fun updateRoutineId(value: Long?) { uiState = uiState.copy(routineId = value, error = null) }
    fun updateHabitId(value: Long?) { uiState = uiState.copy(habitId = value, error = null) }

    fun submit(onSuccess: () -> Unit) {
        val state = uiState
        if (state.title.isBlank()) {
            uiState = state.copy(error = "Title is required.")
            return
        }
        if (state.startTime != null && state.dueTime != null && !state.startTime.isBefore(state.dueTime)) {
            uiState = state.copy(error = "Start time must be before due time.")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(saving = true, error = null)

            when (
                val result = taskRepository.updateTask(
                    taskId = taskId,
                    title = state.title.trim(),
                    description = state.description.trim().ifBlank { null },
                    imageUri = state.imageUri,
                    fileUris = state.fileUris,
                    startTime = state.startTime,
                    dueTime = state.dueTime,
                    repeatType = state.repeatType,
                    scheduledDate = state.scheduledDate,
                    notify = state.notify,
                    selectedDaysOfWeek = state.selectedDaysOfWeek
                )
            ) {
                is TaskUpdateResult.Collision -> {
                    uiState = uiState.copy(
                        saving = false,
                        error = "Clashes with \"${result.with.title}\" " +
                            "(${result.with.startTime}–${result.with.dueTime})."
                    )
                    return@launch
                }
                TaskUpdateResult.Success -> Unit
            }

            if (state.routineId != originalRoutineId) {
                if (state.routineId == null) {
                    routineRepository.removeTaskFromRoutine(taskId)
                } else {
                    when (val assign = routineRepository.assignTask(taskId, state.routineId)) {
                        is TaskAssignResult.Collision -> {
                            uiState = uiState.copy(
                                saving = false,
                                error = "Clashes with \"${assign.with.title}\" in that routine."
                            )
                            return@launch
                        }
                        TaskAssignResult.Success -> Unit
                    }
                }
            }

            if (state.habitId != originalHabitId) {
                if (state.habitId == null) {
                    habitRepository.removeTaskFromHabit(taskId)
                } else {
                    when (val assign = habitRepository.assignTask(taskId, state.habitId)) {
                        is TaskAssignResult.Collision -> {
                            uiState = uiState.copy(
                                saving = false,
                                error = "Clashes with \"${assign.with.title}\" in that habit."
                            )
                            return@launch
                        }
                        TaskAssignResult.Success -> Unit
                    }
                }
            }

            onSuccess()
        }
    }

    fun complete(onDone: () -> Unit) {
        viewModelScope.launch {
            taskRepository.completeTask(taskId)
            onDone()
        }
    }

    fun archive(onDone: () -> Unit) {
        viewModelScope.launch {
            taskRepository.archiveTask(taskId)
            onDone()
        }
    }
}
