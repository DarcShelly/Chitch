package com.chitch.app.domain

import com.chitch.app.data.entity.RepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NotificationSchedulingTest {

    @Test
    fun `one-time task later today schedules for today`() {
        val anchor = LocalDate.of(2026, 3, 10)
        val now = LocalDateTime.of(2026, 3, 10, 8, 0)
        val next = NotificationScheduling.nextOccurrence(RepeatType.ONE_TIME, anchor, LocalTime.of(9, 0), now)
        assertEquals(LocalDateTime.of(2026, 3, 10, 9, 0), next)
    }

    @Test
    fun `one-time task already past has no next occurrence`() {
        val anchor = LocalDate.of(2026, 3, 10)
        val now = LocalDateTime.of(2026, 3, 10, 10, 0)
        val next = NotificationScheduling.nextOccurrence(RepeatType.ONE_TIME, anchor, LocalTime.of(9, 0), now)
        assertNull(next)
    }

    @Test
    fun `one-time task on a future date schedules for that date`() {
        val anchor = LocalDate.of(2026, 3, 15)
        val now = LocalDateTime.of(2026, 3, 10, 10, 0)
        val next = NotificationScheduling.nextOccurrence(RepeatType.ONE_TIME, anchor, LocalTime.of(9, 0), now)
        assertEquals(LocalDateTime.of(2026, 3, 15, 9, 0), next)
    }

    @Test
    fun `daily task rolls to tomorrow once today's time has passed`() {
        val anchor = LocalDate.of(2026, 3, 1)
        val now = LocalDateTime.of(2026, 3, 10, 10, 0)
        val next = NotificationScheduling.nextOccurrence(RepeatType.DAILY, anchor, LocalTime.of(9, 0), now)
        assertEquals(LocalDateTime.of(2026, 3, 11, 9, 0), next)
    }

    @Test
    fun `daily task still ahead today stays today`() {
        val anchor = LocalDate.of(2026, 3, 1)
        val now = LocalDateTime.of(2026, 3, 10, 7, 0)
        val next = NotificationScheduling.nextOccurrence(RepeatType.DAILY, anchor, LocalTime.of(9, 0), now)
        assertEquals(LocalDateTime.of(2026, 3, 10, 9, 0), next)
    }

    @Test
    fun `alternate task lands on the correct two-day bucket`() {
        val anchor = LocalDate.of(2026, 3, 1)
        // Anchor day 0, so day 9 (Mar 10) is an "off" bucket day (9 is odd) -> next bucket Mar 11.
        val now = LocalDateTime.of(2026, 3, 10, 10, 0)
        val next = NotificationScheduling.nextOccurrence(RepeatType.ALTERNATE, anchor, LocalTime.of(9, 0), now)
        assertEquals(LocalDateTime.of(2026, 3, 11, 9, 0), next)
    }

    @Test
    fun `weekly task rolls a full week forward`() {
        val anchor = LocalDate.of(2026, 3, 1)
        val now = LocalDateTime.of(2026, 3, 1, 10, 0)
        val next = NotificationScheduling.nextOccurrence(RepeatType.WEEKLY, anchor, LocalTime.of(9, 0), now)
        assertEquals(LocalDateTime.of(2026, 3, 8, 9, 0), next)
    }

    @Test
    fun `monthly task anchored on the 31st clamps to shorter months`() {
        val anchor = LocalDate.of(2026, 1, 31)
        val now = LocalDateTime.of(2026, 2, 1, 0, 0)
        val next = NotificationScheduling.nextOccurrence(RepeatType.MONTHLY, anchor, LocalTime.of(9, 0), now)
        assertEquals(LocalDateTime.of(2026, 2, 28, 9, 0), next)
    }

    @Test
    fun `reference date before the anchor schedules at the anchor itself`() {
        val anchor = LocalDate.of(2026, 4, 1)
        val now = LocalDateTime.of(2026, 3, 10, 10, 0)
        val next = NotificationScheduling.nextOccurrence(RepeatType.WEEKLY, anchor, LocalTime.of(9, 0), now)
        assertEquals(LocalDateTime.of(2026, 4, 1, 9, 0), next)
    }
}
