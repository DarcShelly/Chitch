package com.chitch.app.domain

import com.chitch.app.data.entity.DailyReviewEntity
import com.chitch.app.data.entity.MoodRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DailySummaryAggregatorTest {

    private fun review(
        date: LocalDate,
        mood: MoodRating,
        tasksDone: Int = 0,
        tasksMissed: Int = 0,
        habitsChecked: Int = 0,
        habitsMissed: Int = 0
    ) = DailyReviewEntity(
        date = date,
        mood = mood,
        tasksDoneCount = tasksDone,
        tasksMissedCount = tasksMissed,
        habitsCheckedCount = habitsChecked,
        habitsMissedCount = habitsMissed,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH
    )

    @Test
    fun `empty list yields empty summary`() {
        val summary = DailySummaryAggregator.aggregate(emptyList())
        assertEquals(0, summary.dayCount)
        assertNull(summary.averageMood)
        assertNull(summary.bestDay)
        assertNull(summary.toughestDay)
    }

    @Test
    fun `aggregates totals and mood extremes across days`() {
        val d1 = LocalDate.of(2026, 3, 1)
        val d2 = LocalDate.of(2026, 3, 2)
        val d3 = LocalDate.of(2026, 3, 3)
        val reviews = listOf(
            review(d1, MoodRating.SAD, tasksDone = 2, tasksMissed = 1, habitsChecked = 1, habitsMissed = 1),
            review(d2, MoodRating.VERY_HAPPY, tasksDone = 5, tasksMissed = 0, habitsChecked = 2, habitsMissed = 0),
            review(d3, MoodRating.NEUTRAL, tasksDone = 3, tasksMissed = 2, habitsChecked = 1, habitsMissed = 0)
        )

        val summary = DailySummaryAggregator.aggregate(reviews)

        assertEquals(3, summary.dayCount)
        assertEquals(10, summary.totalTasksDone)
        assertEquals(3, summary.totalTasksMissed)
        assertEquals(4, summary.totalHabitsChecked)
        assertEquals(1, summary.totalHabitsMissed)
        assertEquals(d2, summary.bestDay)
        assertEquals(d1, summary.toughestDay)
        assertEquals(
            (MoodRating.SAD.ordinal + MoodRating.VERY_HAPPY.ordinal + MoodRating.NEUTRAL.ordinal) / 3.0,
            summary.averageMood!!,
            0.0001
        )
    }
}
