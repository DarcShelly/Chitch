package com.chitch.app.domain

import com.chitch.app.data.entity.RepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class TaskRecurrenceTest {

    private val zone: ZoneId = ZoneOffset.UTC

    private fun instantAt(date: LocalDate, hour: Int) =
        date.atTime(hour, 0).atZone(zone).toInstant()

    @Test
    fun `daily period is always today`() {
        val anchor = LocalDate.of(2026, 1, 1)
        val today = LocalDate.of(2026, 3, 10)
        assertEquals(today, TaskRecurrence.currentPeriodStart(RepeatType.DAILY, anchor, today))
    }

    @Test
    fun `completing before start time still counts for today (premark)`() {
        val anchor = LocalDate.of(2026, 3, 10)
        val today = LocalDate.of(2026, 3, 10)
        // Completed at 6am, well before a hypothetical 9am start time — should still count.
        val completedAt = instantAt(today, hour = 6)
        assertTrue(
            TaskRecurrence.isCompletedForCurrentPeriod(completedAt, RepeatType.DAILY, anchor, today, zone)
        )
    }

    @Test
    fun `daily task resets the next day`() {
        val anchor = LocalDate.of(2026, 3, 10)
        val completedYesterday = instantAt(LocalDate.of(2026, 3, 10), hour = 20)
        val today = LocalDate.of(2026, 3, 11)
        assertFalse(
            TaskRecurrence.isCompletedForCurrentPeriod(completedYesterday, RepeatType.DAILY, anchor, today, zone)
        )
    }

    @Test
    fun `alternate day bucket groups two days together`() {
        val anchor = LocalDate.of(2026, 3, 1) // bucket 0: Mar 1-2, bucket 1: Mar 3-4, ...
        assertEquals(anchor, TaskRecurrence.currentPeriodStart(RepeatType.ALTERNATE, anchor, LocalDate.of(2026, 3, 1)))
        assertEquals(anchor, TaskRecurrence.currentPeriodStart(RepeatType.ALTERNATE, anchor, LocalDate.of(2026, 3, 2)))
        assertEquals(
            LocalDate.of(2026, 3, 3),
            TaskRecurrence.currentPeriodStart(RepeatType.ALTERNATE, anchor, LocalDate.of(2026, 3, 3))
        )
    }

    @Test
    fun `alternate task completed in current bucket does not reset until next bucket`() {
        val anchor = LocalDate.of(2026, 3, 1)
        val completedDay1 = instantAt(LocalDate.of(2026, 3, 1), hour = 10)
        assertTrue(
            TaskRecurrence.isCompletedForCurrentPeriod(
                completedDay1, RepeatType.ALTERNATE, anchor, LocalDate.of(2026, 3, 2), zone
            )
        )
        assertFalse(
            TaskRecurrence.isCompletedForCurrentPeriod(
                completedDay1, RepeatType.ALTERNATE, anchor, LocalDate.of(2026, 3, 3), zone
            )
        )
    }

    @Test
    fun `weekly bucket is a rolling 7-day window from anchor`() {
        val anchor = LocalDate.of(2026, 3, 2) // Monday
        assertEquals(anchor, TaskRecurrence.currentPeriodStart(RepeatType.WEEKLY, anchor, LocalDate.of(2026, 3, 8)))
        assertEquals(
            LocalDate.of(2026, 3, 9),
            TaskRecurrence.currentPeriodStart(RepeatType.WEEKLY, anchor, LocalDate.of(2026, 3, 9))
        )
    }

    @Test
    fun `monthly period anchors to day-of-month and clamps short months`() {
        val anchor = LocalDate.of(2026, 1, 31)
        // February has 28 days in 2026 (not a leap year) -> clamp to Feb 28.
        assertEquals(
            LocalDate.of(2026, 2, 28),
            TaskRecurrence.currentPeriodStart(RepeatType.MONTHLY, anchor, LocalDate.of(2026, 2, 28))
        )
    }

    @Test
    fun `monthly period before this month's anchor day falls back to previous month`() {
        val anchor = LocalDate.of(2026, 1, 15)
        val today = LocalDate.of(2026, 2, 10) // before Feb 15
        assertEquals(
            LocalDate.of(2026, 1, 15),
            TaskRecurrence.currentPeriodStart(RepeatType.MONTHLY, anchor, today)
        )
    }

    @Test
    fun `one-time task counts as completed forever once done`() {
        val anchor = LocalDate.of(2026, 1, 1)
        val completedAt = instantAt(anchor, hour = 1)
        assertTrue(
            TaskRecurrence.isCompletedForCurrentPeriod(
                completedAt, RepeatType.ONE_TIME, anchor, LocalDate.of(2026, 6, 1), zone
            )
        )
    }

    @Test
    fun `never completed is never completed`() {
        assertFalse(
            TaskRecurrence.isCompletedForCurrentPeriod(
                null, RepeatType.DAILY, LocalDate.now(), LocalDate.now(), zone
            )
        )
    }
}
