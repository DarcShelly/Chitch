package com.chitch.app.ui.routine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitch.app.data.entity.RepeatType
import com.chitch.app.data.repository.RoutineCreationResult
import com.chitch.app.data.repository.RoutineRepository
import com.chitch.app.data.repository.RoutineTaskDraft
import com.chitch.app.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class RoutineTaskRow(
    val rowId: Int,
    val title: String = "",
    val startTime: LocalTime? = null,
    val dueTime: LocalTime? = null,
    val repeatType: RepeatType = RepeatType.DAILY,
    val scheduledDate: LocalDate? = null
)

data class CreateRoutineUiState(
    val title: String = "",
    val description: String = "",
    val imageUri: String? = null,
    val fileUris: List<String> = emptyList(),
    val activateNow: Boolean = true,
    val tasks: List<RoutineTaskRow> = listOf(RoutineTaskRow(rowId = 0)),
    val saving: Boolean = false,
    val error: String? = null
)

class CreateRoutineViewModel(
    private val routineRepository: RoutineRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    var uiState by mutableStateOf(CreateRoutineUiState())
        private set

    private var nextRowId = 1

    fun updateTitle(value: String) { uiState = uiState.copy(title = value, error = null) }
    fun updateDescription(value: String) { uiState = uiState.copy(description = value) }
    fun updateImage(uri: String?) { uiState = uiState.copy(imageUri = uri) }
    fun addFiles(uris: List<String>) { uiState = uiState.copy(fileUris = uiState.fileUris + uris) }
    fun removeFile(uri: String) { uiState = uiState.copy(fileUris = uiState.fileUris.filterNot { it == uri }) }
    fun updateActivateNow(value: Boolean) { uiState = uiState.copy(activateNow = value) }

    fun addTaskRow() {
        uiState = uiState.copy(tasks = uiState.tasks + RoutineTaskRow(rowId = nextRowId++))
    }

    /** Replaces the form's title/description/tasks with [template]'s — still fully editable after. */
    fun applyTemplate(template: RoutineTemplate) {
        val newTasks = template.tasks.map { task ->
            RoutineTaskRow(
                rowId = nextRowId++,
                title = task.title,
                startTime = task.startTime,
                dueTime = task.dueTime,
                repeatType = task.repeatType
            )
        }
        uiState = uiState.copy(
            title = template.title,
            description = template.description,
            tasks = newTasks,
            error = null
        )
    }

    fun removeTaskRow(rowId: Int) {
        if (uiState.tasks.size <= 1) return
        uiState = uiState.copy(tasks = uiState.tasks.filterNot { it.rowId == rowId })
    }

    fun updateRowTitle(rowId: Int, value: String) = updateRow(rowId) { it.copy(title = value) }
    fun updateRowStartTime(rowId: Int, value: LocalTime?) = updateRow(rowId) { it.copy(startTime = value) }
    fun updateRowDueTime(rowId: Int, value: LocalTime?) = updateRow(rowId) { it.copy(dueTime = value) }
    fun updateRowRepeatType(rowId: Int, value: RepeatType) = updateRow(rowId) { it.copy(repeatType = value) }
    fun updateRowScheduledDate(rowId: Int, value: LocalDate?) = updateRow(rowId) { it.copy(scheduledDate = value) }

    private fun updateRow(rowId: Int, transform: (RoutineTaskRow) -> RoutineTaskRow) {
        uiState = uiState.copy(
            tasks = uiState.tasks.map { if (it.rowId == rowId) transform(it) else it },
            error = null
        )
    }

    fun submit(onSuccess: () -> Unit) {
        val state = uiState
        if (state.title.isBlank()) {
            uiState = state.copy(error = "Routine title is required.")
            return
        }
        if (state.tasks.any { it.title.isBlank() || it.startTime == null || it.dueTime == null }) {
            uiState = state.copy(error = "Every routine task needs a title, start time, and due time.")
            return
        }
        if (state.tasks.any { !it.startTime!!.isBefore(it.dueTime!!) }) {
            uiState = state.copy(error = "Each task's start time must be before its due time.")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(saving = true, error = null)
            val drafts = state.tasks.map {
                RoutineTaskDraft(
                    title = it.title.trim(),
                    startTime = it.startTime!!,
                    dueTime = it.dueTime!!,
                    repeatType = it.repeatType,
                    scheduledDate = it.scheduledDate
                )
            }
            when (
                val result = routineRepository.createRoutine(
                    title = state.title.trim(),
                    description = state.description.trim().ifBlank { null },
                    imageUri = state.imageUri,
                    fileUris = state.fileUris,
                    activateNow = state.activateNow,
                    taskDrafts = drafts
                )
            ) {
                is RoutineCreationResult.Success -> {
                    uiState = CreateRoutineUiState()
                    onSuccess()
                }
                is RoutineCreationResult.InternalCollision -> uiState = uiState.copy(
                    saving = false,
                    error = "\"${result.a.title}\" and \"${result.b.title}\" overlap in time."
                )
                is RoutineCreationResult.HoursExceeded -> uiState = uiState.copy(
                    saving = false,
                    error = "This routine spans %.1f hours but only %.1f free hours are available "
                        .format(result.spanHours, result.maxAllowedHours) +
                        "after sleep and working hours. Adjust it in Settings or shorten the routine."
                )
                is RoutineCreationResult.ExternalCollision -> uiState = uiState.copy(
                    saving = false,
                    error = "\"${result.with.title}\" already occupies that time."
                )
                RoutineCreationResult.AnotherRoutineActive -> uiState = uiState.copy(
                    saving = false,
                    error = "Another routine is already active. Deactivate it first, or save this as a draft."
                )
            }
        }
    }
}
