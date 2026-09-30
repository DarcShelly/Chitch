package com.chitch.app.ui.routine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.repository.RoutineRepository
import com.chitch.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditRoutineUiState(
    val title: String = "",
    val description: String = "",
    val imageUri: String? = null,
    val fileUris: List<String> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val showTaskPicker: Boolean = false
)

/** Metadata edit (title/description/attachments) plus task-list management. */
class EditRoutineViewModel(
    private val routineId: Long,
    private val routineRepository: RoutineRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.observeByRoutine(routineId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Tasks eligible to attach: not already in a routine or a habit. */
    val availableTasks: StateFlow<List<TaskEntity>> = taskRepository.observeAllTasksRaw()
        .map { all -> all.filter { it.routineId == null && it.habitId == null } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var uiState by mutableStateOf(EditRoutineUiState())
        private set

    init {
        viewModelScope.launch {
            val routine = routineRepository.getById(routineId)
            uiState = if (routine != null) {
                EditRoutineUiState(
                    title = routine.title,
                    description = routine.description.orEmpty(),
                    imageUri = routine.imageUri,
                    fileUris = routine.fileUris,
                    loading = false
                )
            } else {
                uiState.copy(loading = false, error = "Routine not found.")
            }
        }
    }

    fun updateTitle(value: String) { uiState = uiState.copy(title = value, error = null) }
    fun updateDescription(value: String) { uiState = uiState.copy(description = value) }
    fun updateImage(uri: String?) { uiState = uiState.copy(imageUri = uri) }
    fun addFiles(uris: List<String>) { uiState = uiState.copy(fileUris = uiState.fileUris + uris) }
    fun removeFile(uri: String) { uiState = uiState.copy(fileUris = uiState.fileUris.filterNot { it == uri }) }

    fun openTaskPicker() { uiState = uiState.copy(showTaskPicker = true) }
    fun closeTaskPicker() { uiState = uiState.copy(showTaskPicker = false) }

    fun addExistingTask(taskId: Long) {
        viewModelScope.launch {
            routineRepository.assignTask(taskId, routineId)
            uiState = uiState.copy(showTaskPicker = false)
        }
    }

    fun removeTask(taskId: Long) {
        viewModelScope.launch { routineRepository.removeTaskFromRoutine(taskId) }
    }

    fun reorderTasks(newOrder: List<TaskEntity>) {
        viewModelScope.launch { taskRepository.reorderTasks(newOrder.map { it.id }) }
    }

    fun submit(onSuccess: () -> Unit) {
        val state = uiState
        if (state.title.isBlank()) {
            uiState = state.copy(error = "Routine title is required.")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(saving = true, error = null)
            routineRepository.updateRoutine(
                routineId = routineId,
                title = state.title.trim(),
                description = state.description.trim().ifBlank { null },
                imageUri = state.imageUri,
                fileUris = state.fileUris
            )
            onSuccess()
        }
    }
}
