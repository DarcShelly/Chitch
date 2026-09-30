package com.chitch.app.ui.habit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitch.app.data.entity.RepeatType
import com.chitch.app.data.repository.HabitCreationResult
import com.chitch.app.data.repository.HabitRepository
import com.chitch.app.data.repository.HabitTaskDraft
import com.chitch.app.data.repository.MAX_ACTIVE_HABITS
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class HabitTaskRow(
    val rowId: Int,
    val title: String = "",
    val hasTime: Boolean = false,
    val startTime: LocalTime? = null,
    val dueTime: LocalTime? = null,
    val repeatType: RepeatType = RepeatType.DAILY,
    val scheduledDate: LocalDate? = null
)

data class CreateHabitUiState(
    val title: String = "",
    val description: String = "",
    val imageUri: String? = null,
    val fileUris: List<String> = emptyList(),
    val activateNow: Boolean = true,
    val tasks: List<HabitTaskRow> = listOf(HabitTaskRow(rowId = 0)),
    val saving: Boolean = false,
    val error: String? = null
)

class CreateHabitViewModel(private val habitRepository: HabitRepository) : ViewModel() {

    var uiState by mutableStateOf(CreateHabitUiState())
        private set

    private var nextRowId = 1

    fun updateTitle(value: String) { uiState = uiState.copy(title = value, error = null) }
    fun updateDescription(value: String) { uiState = uiState.copy(description = value) }
    fun updateImage(uri: String?) { uiState = uiState.copy(imageUri = uri) }
    fun addFiles(uris: List<String>) { uiState = uiState.copy(fileUris = uiState.fileUris + uris) }
    fun removeFile(uri: String) { uiState = uiState.copy(fileUris = uiState.fileUris.filterNot { it == uri }) }
    fun updateActivateNow(value: Boolean) { uiState = uiState.copy(activateNow = value) }

    fun addTaskRow() {
        uiState = uiState.copy(tasks = uiState.tasks + HabitTaskRow(rowId = nextRowId++))
    }

    /** Replaces the form's title/description/tasks with [template]'s — still fully editable after. */
    fun applyTemplate(template: HabitTemplate) {
        val newTasks = template.tasks.map { task -> HabitTaskRow(rowId = nextRowId++, title = task.title) }
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
    fun updateRowHasTime(rowId: Int, value: Boolean) = updateRow(rowId) {
        if (value) it.copy(hasTime = true) else it.copy(hasTime = false, startTime = null, dueTime = null)
    }
    fun updateRowStartTime(rowId: Int, value: LocalTime?) = updateRow(rowId) { it.copy(startTime = value) }
    fun updateRowDueTime(rowId: Int, value: LocalTime?) = updateRow(rowId) { it.copy(dueTime = value) }
    fun updateRowRepeatType(rowId: Int, value: RepeatType) = updateRow(rowId) { it.copy(repeatType = value) }
    fun updateRowScheduledDate(rowId: Int, value: LocalDate?) = updateRow(rowId) { it.copy(scheduledDate = value) }

    private fun updateRow(rowId: Int, transform: (HabitTaskRow) -> HabitTaskRow) {
        uiState = uiState.copy(
            tasks = uiState.tasks.map { if (it.rowId == rowId) transform(it) else it },
            error = null
        )
    }

    fun submit(onSuccess: () -> Unit) {
        val state = uiState
        if (state.title.isBlank()) {
            uiState = state.copy(error = "Habit title is required.")
            return
        }
        if (state.tasks.any { it.title.isBlank() }) {
            uiState = state.copy(error = "Every habit task needs a title.")
            return
        }
        if (state.tasks.any { it.hasTime && (it.startTime == null || it.dueTime == null) }) {
            uiState = state.copy(error = "Set both a start and end time, or turn timing off for that task.")
            return
        }
        if (state.tasks.any { it.hasTime && !it.startTime!!.isBefore(it.dueTime!!) }) {
            uiState = state.copy(error = "Each task's start time must be before its end time.")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(saving = true, error = null)
            val drafts = state.tasks.map { row ->
                HabitTaskDraft(
                    title = row.title.trim(),
                    startTime = if (row.hasTime) row.startTime else null,
                    dueTime = if (row.hasTime) row.dueTime else null,
                    repeatType = row.repeatType,
                    scheduledDate = row.scheduledDate
                )
            }
            when (
                val result = habitRepository.createHabit(
                    title = state.title.trim(),
                    description = state.description.trim().ifBlank { null },
                    imageUri = state.imageUri,
                    fileUris = state.fileUris,
                    activateNow = state.activateNow,
                    taskDrafts = drafts
                )
            ) {
                is HabitCreationResult.Success -> {
                    uiState = CreateHabitUiState()
                    onSuccess()
                }
                is HabitCreationResult.InternalCollision -> uiState = uiState.copy(
                    saving = false,
                    error = "\"${result.a.title}\" and \"${result.b.title}\" overlap in time."
                )
                is HabitCreationResult.ExternalCollision -> uiState = uiState.copy(
                    saving = false,
                    error = "\"${result.with.title}\" already occupies that time."
                )
                HabitCreationResult.TooManyActiveHabits -> uiState = uiState.copy(
                    saving = false,
                    error = "Only $MAX_ACTIVE_HABITS habits can be active at a time. " +
                        "Archive or complete one first, or save this as a draft."
                )
            }
        }
    }
}
