package com.chitch.app.ui.task

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitch.app.data.entity.RepeatType
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.repository.TaskCreationResult
import com.chitch.app.data.repository.TaskRepository
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class CreateTaskUiState(
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
    val saving: Boolean = false,
    val error: String? = null
)

class CreateTaskViewModel(
    private val repository: TaskRepository,
    private val routineId: Long? = null,
    private val habitId: Long? = null
) : ViewModel() {

    var uiState by mutableStateOf(CreateTaskUiState())
        private set

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
    fun updateScheduledDate(value: LocalDate?) { uiState = uiState.copy(scheduledDate = value, error = null) }
    fun updateNotify(value: Boolean) { uiState = uiState.copy(notify = value) }

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
            val now = Instant.now()
            val draft = TaskEntity(
                title = state.title.trim(),
                description = state.description.trim().ifBlank { null },
                imageUri = state.imageUri,
                fileUris = state.fileUris,
                routineId = routineId,
                habitId = habitId,
                startTime = state.startTime,
                dueTime = state.dueTime,
                scheduledDate = state.scheduledDate,
                repeatType = state.repeatType,
                selectedDaysOfWeek = state.selectedDaysOfWeek,
                notify = state.notify,
                createdAt = now,
                updatedAt = now
            )
            when (val result = repository.createTask(draft)) {
                is TaskCreationResult.Success -> {
                    uiState = CreateTaskUiState()
                    onSuccess()
                }
                is TaskCreationResult.Collision -> {
                    uiState = uiState.copy(
                        saving = false,
                        error = "Clashes with \"${result.with.title}\" " +
                            "(${result.with.startTime}–${result.with.dueTime})."
                    )
                }
            }
        }
    }
}
