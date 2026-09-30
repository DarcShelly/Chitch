package com.chitch.app.domain

import com.chitch.app.data.entity.RepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Day-of-week-aware WEEKLY behavior — opt-in via a non-empty selectedDaysOfWeek, see TaskRecurrence.kt. */
class WeeklyDayOfWeekRecurrenceTest {

    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 3, 2)
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)
    private val thursday = monday.plusDays(3)
    private val friday = monday.plusDays(4)
    private val saturday = monday.plusDays(5)
    private val nextMonday = monday.plusDays(7)

    private fun instantAt(date: LocalDate, hour: Int) = date.atTime(hour, 0).atZone(zone).toInstant()

    @Test
    fun `period start is the most recent selected weekday, not a rolling bucket`() {
        val selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        assertEquals(
            wednesday,
            TaskRecurrence.currentPeriodStart(RepeatType.WEEKLY, monday, thursday, selectedDays)
        )
        assertEquals(
            friday,
            TaskRecurrence.currentPeriodStart(RepeatType.WEEKLY, monday, saturday, selectedDays)
        )
    }

    @Test
    fun `empty selectedDaysOfWeek keeps the legacy rolling 7-day bucket`() {
        assertEquals(
            monday,
            TaskRecurrence.currentPeriodStart(RepeatType.WEEKLY, monday, saturday, emptySet())
        )
    }

    @Test
    fun `completing on a due day counts until the next due day, then resets`() {
        val selectedDays = setOf(DayOfWeek.MONDAY)
        val completedMonday = instantAt(monday, hour = 9)

        assertTrue(
            TaskRecurrence.isCompletedForCurrentPeriod(
                completedMonday, RepeatType.WEEKLY, monday, tuesday, zone, selectedDays
            )
        )
        assertFalse(
            TaskRecurrence.isCompletedForCurrentPeriod(
                completedMonday, RepeatType.WEEKLY, monday, nextMonday, zone, selectedDays
            )
        )
    }

    @Test
    fun `a non-due day is not evaluated as missed`() {
        val selectedDays = setOf(DayOfWeek.MONDAY)
        // anchorDate's own Monday is the first-ever due day, so nothing has elapsed to evaluate
        // until we're past the *next* Monday too — evaluate from there.
        val result = MissedTaskEvaluator.evaluate(
            repeatType = RepeatType.WEEKLY,
            anchorDate = monday,
            lastCompletedAt = null,
            lastMissEvaluatedDate = null,
            today = nextMonday,
            zoneId = zone,
            selectedDaysOfWeek = selectedDays
        )
        assertTrue(result.isMissed)
        assertEquals(monday, result.newLastMissEvaluatedDate)

        // Re-evaluating on a later non-due day before the Monday after that must not double-count.
        val second = MissedTaskEvaluator.evaluate(
            repeatType = RepeatType.WEEKLY,
            anchorDate = monday,
            lastCompletedAt = null,
            lastMissEvaluatedDate = result.newLastMissEvaluatedDate,
            today = nextMonday.plusDays(1),
            zoneId = zone,
            selectedDaysOfWeek = selectedDays
        )
        assertFalse(second.isMissed)
    }

    @Test
    fun `completing on the due day before it elapses avoids a miss`() {
        val selectedDays = setOf(DayOfWeek.MONDAY)
        val completedMonday: Instant = instantAt(monday, hour = 9)

        val result = MissedTaskEvaluator.evaluate(
            repeatType = RepeatType.WEEKLY,
            anchorDate = monday,
            lastCompletedAt = completedMonday,
            lastMissEvaluatedDate = null,
            today = nextMonday,
            zoneId = zone,
            selectedDaysOfWeek = selectedDays
        )
        assertFalse(result.isMissed)
    }
}
