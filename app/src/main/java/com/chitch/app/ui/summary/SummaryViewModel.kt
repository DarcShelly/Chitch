package com.chitch.app.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitch.app.data.repository.DailyReviewRepository
import com.chitch.app.data.repository.SummaryRange
import com.chitch.app.domain.PeriodSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val EMPTY_SUMMARY = PeriodSummary(
    dayCount = 0,
    averageMood = null,
    moodCounts = emptyMap(),
    totalTasksDone = 0,
    totalTasksMissed = 0,
    totalHabitsChecked = 0,
    totalHabitsMissed = 0,
    bestDay = null,
    toughestDay = null
)

class SummaryViewModel(private val dailyReviewRepository: DailyReviewRepository) : ViewModel() {

    private val _range = MutableStateFlow(SummaryRange.WEEK)
    val range: StateFlow<SummaryRange> = _range.asStateFlow()

    private val _summary = MutableStateFlow(EMPTY_SUMMARY)
    val summary: StateFlow<PeriodSummary> = _summary.asStateFlow()

    init {
        load()
    }

    fun selectRange(range: SummaryRange) {
        _range.value = range
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _summary.value = dailyReviewRepository.getSummary(_range.value)
        }
    }
}
