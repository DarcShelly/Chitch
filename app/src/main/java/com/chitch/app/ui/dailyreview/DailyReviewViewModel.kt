package com.chitch.app.ui.dailyreview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitch.app.data.entity.HabitEntity
import com.chitch.app.data.entity.MoodRating
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.repository.DailyReviewRepository
import com.chitch.app.data.repository.HabitRepository
import com.chitch.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DailyReviewUiState(
    val loading: Boolean = true,
    val mood: MoodRating = MoodRating.NEUTRAL,
    val notesForTomorrow: String = "",
    val thingsLiked: String = "",
    val thingsMissed: String = "",
    val checkedHabitIds: Set<Long> = emptySet(),
    val alreadySubmittedToday: Boolean = false,
    val submitting: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val celebrateHabits: List<HabitEntity> = emptyList()
)

class DailyReviewViewModel(
    private val dailyReviewRepository: DailyReviewRepository,
    private val taskRepository: TaskRepository,
    habitRepository: HabitRepository
) : ViewModel() {

    val today: LocalDate = LocalDate.now()

    val activeTasks: StateFlow<List<TaskEntity>> = taskRepository.observeAllActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeHabits: StateFlow<List<HabitEntity>> = habitRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var uiState by mutableStateOf(DailyReviewUiState())
        private set

    init {
        viewModelScope.launch {
            val existing = dailyReviewRepository.getReview(today)
            val checkedIds = dailyReviewRepository.getCheckedHabitIds(today)
            uiState = if (existing != null) {
                uiState.copy(
                    loading = false,
                    mood = existing.mood,
                    notesForTomorrow = existing.notesForTomorrow.orEmpty(),
                    thingsLiked = existing.thingsLiked.orEmpty(),
                    thingsMissed = existing.thingsMissed.orEmpty(),
                    checkedHabitIds = checkedIds,
                    alreadySubmittedToday = true
                )
            } else {
                uiState.copy(loading = false, checkedHabitIds = checkedIds)
            }
        }
    }

    fun updateMood(mood: MoodRating) {
        uiState = uiState.copy(mood = mood)
    }

    fun updateNotesForTomorrow(value: String) {
        uiState = uiState.copy(notesForTomorrow = value)
    }

    fun updateThingsLiked(value: String) {
        uiState = uiState.copy(thingsLiked = value)
    }

    fun updateThingsMissed(value: String) {
        uiState = uiState.copy(thingsMissed = value)
    }

    fun toggleHabitChecked(habitId: Long) {
        val current = uiState.checkedHabitIds
        uiState = uiState.copy(checkedHabitIds = if (habitId in current) current - habitId else current + habitId)
    }

    fun toggleTask(taskId: Long, currentlyDone: Boolean) {
        viewModelScope.launch {
            if (currentlyDone) taskRepository.uncompleteTask(taskId) else taskRepository.completeTask(taskId)
        }
    }

    fun submit() {
        val state = uiState
        viewModelScope.launch {
            uiState = uiState.copy(submitting = true)
            val result = dailyReviewRepository.submitReview(
                date = today,
                mood = state.mood,
                notesForTomorrow = state.notesForTomorrow,
                thingsLiked = state.thingsLiked,
                thingsMissed = state.thingsMissed,
                checkedHabitIds = state.checkedHabitIds
            )
            uiState = uiState.copy(
                submitting = false,
                alreadySubmittedToday = true,
                suggestions = result.suggestions,
                celebrateHabits = result.completedHabits
            )
        }
    }

    fun dismissCelebration() {
        uiState = uiState.copy(celebrateHabits = emptyList())
    }
}
