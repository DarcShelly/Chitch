package com.chitch.app.domain

import com.chitch.app.data.entity.DailyReviewEntity
import com.chitch.app.data.entity.MoodRating
import java.time.LocalDate

data class PeriodSummary(
    val dayCount: Int,
    val averageMood: Double?,
    val moodCounts: Map<MoodRating, Int>,
    val totalTasksDone: Int,
    val totalTasksMissed: Int,
    val totalHabitsChecked: Int,
    val totalHabitsMissed: Int,
    val bestDay: LocalDate?,
    val toughestDay: LocalDate?
)

/** Rolls up a range of [DailyReviewEntity] snapshots into period totals — pure, unit-testable. */
object DailySummaryAggregator {

    fun aggregate(reviews: List<DailyReviewEntity>): PeriodSummary {
        if (reviews.isEmpty()) {
            return PeriodSummary(
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
        }

        val moodCounts = reviews.groupingBy { it.mood }.eachCount()
        val averageMood = reviews.map { it.mood.ordinal }.average()
        val best = reviews.maxByOrNull { it.mood.ordinal }
        val toughest = reviews.minByOrNull { it.mood.ordinal }

        return PeriodSummary(
            dayCount = reviews.size,
            averageMood = averageMood,
            moodCounts = moodCounts,
            totalTasksDone = reviews.sumOf { it.tasksDoneCount },
            totalTasksMissed = reviews.sumOf { it.tasksMissedCount },
            totalHabitsChecked = reviews.sumOf { it.habitsCheckedCount },
            totalHabitsMissed = reviews.sumOf { it.habitsMissedCount },
            bestDay = best?.date,
            toughestDay = toughest?.date
        )
    }
}
