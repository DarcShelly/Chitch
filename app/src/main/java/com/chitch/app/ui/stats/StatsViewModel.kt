package com.chitch.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitch.app.data.entity.HabitEntity
import com.chitch.app.data.entity.TaskLedgerEntity
import com.chitch.app.data.repository.DailyHabitCheckRepository
import com.chitch.app.data.repository.HabitRepository
import com.chitch.app.data.repository.TaskLedgerRepository
import com.chitch.app.domain.StreakCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HabitStreakItem(val habit: HabitEntity, val currentStreak: Int)

class StatsViewModel(
    ledgerRepository: TaskLedgerRepository,
    habitRepository: HabitRepository,
    dailyHabitCheckRepository: DailyHabitCheckRepository
) : ViewModel() {
    val ledger: StateFlow<TaskLedgerEntity> = ledgerRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskLedgerEntity())

    val habitStreaks: StateFlow<List<HabitStreakItem>> = habitRepository.observeAll()
        .map { habits ->
            val today = LocalDate.now()
            habits.map { habit ->
                val doneDates = dailyHabitCheckRepository.getDoneDates(habit.id, habit.streakWindowStartDate, today)
                HabitStreakItem(habit, StreakCalculator.currentStreak(doneDates, today))
            }.sortedByDescending { it.currentStreak }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
