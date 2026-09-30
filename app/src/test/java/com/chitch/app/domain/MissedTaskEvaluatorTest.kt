package com.chitch.app.domain

import com.chitch.app.data.entity.RepeatType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MissedTaskEvaluatorTest {

    private val zone = ZoneOffset.UTC

    private fun instantAt(date: LocalDate) = date.atTime(10, 0).atZone(zone).toInstant()

    @Test
    fun `one-time task not yet due is not missed`() {
        val anchor = LocalDate.of(2026, 3, 10)
        val result = MissedTaskEvaluator.evaluate(
            RepeatType.ONE_TIME, anchor, lastCompletedAt = null, lastMissEvaluatedDate = null,
            today = LocalDate.of(2026, 3, 10), zoneId = zone
        )
        assertFalse(result.isMissed)
        assertEquals(null, result.newLastMissEvaluatedDate)
    }

    @Test
    fun `one-time task uncompleted after its day is missed exactly once`() {
        val anchor = LocalDate.of(2026, 3, 10)
        val first = MissedTaskEvaluator.evaluate(
            RepeatType.ONE_TIME, anchor, lastCompletedAt = null, lastMissEvaluatedDate = null,
            today = LocalDate.of(2026, 3, 11), zoneId = zone
        )
        assertTrue(first.isMissed)
        assertEquals(anchor, first.newLastMissEvaluatedDate)

        // Re-evaluating with the watermark now set must not double-count.
        val second = MissedTaskEvaluator.evaluate(
            RepeatType.ONE_TIME, anchor, lastCompletedAt = null, lastMissEvaluatedDate = first.newLastMissEvaluatedDate,
            today = LocalDate.of(2026, 3, 12), zoneId = zone
        )
        assertFalse(second.isMissed)
    }

    @Test
    fun `one-time task completed before its day passed is not missed`() {
        val anchor = LocalDate.of(2026, 3, 10)
        val result = MissedTaskEvaluator.evaluate(
            RepeatType.ONE_TIME, anchor, lastCompletedAt = instantAt(anchor), lastMissEvaluatedDate = null,
            today = LocalDate.of(2026, 3, 11), zoneId = zone
        )
        assertFalse(result.isMissed)
    }

    @Test
    fun `daily task never completed is missed once the day ends`() {
        val anchor = LocalDate.of(2026, 3, 10)
        val result = MissedTaskEvaluator.evaluate(
            RepeatType.DAILY, anchor, lastCompletedAt = null, lastMissEvaluatedDate = null,
            today = LocalDate.of(2026, 3, 11), zoneId = zone
        )
        assertTrue(result.isMissed)
        assertEquals(LocalDate.of(2026, 3, 10), result.newLastMissEvaluatedDate)
    }

    @Test
    fun `daily task completed yesterday is not missed`() {
        val anchor = LocalDate.of(2026, 3, 10)
        val result = MissedTaskEvaluator.evaluate(
            RepeatType.DAILY, anchor,
            lastCompletedAt = instantAt(LocalDate.of(2026, 3, 10)),
            lastMissEvaluatedDate = null,
            today = LocalDate.of(2026, 3, 11), zoneId = zone
        )
        assertFalse(result.isMissed)
    }

    @Test
    fun `daily task does not double-count the same missed day`() {
        val anchor = LocalDate.of(2026, 3, 10)
        val first = MissedTaskEvaluator.evaluate(
            RepeatType.DAILY, anchor, lastCompletedAt = null, lastMissEvaluatedDate = null,
            today = LocalDate.of(2026, 3, 11), zoneId = zone
        )
        assertTrue(first.isMissed)

        val second = MissedTaskEvaluator.evaluate(
            RepeatType.DAILY, anchor, lastCompletedAt = null,
            lastMissEvaluatedDate = first.newLastMissEvaluatedDate,
            today = LocalDate.of(2026, 3, 11), zoneId = zone
        )
        assertFalse(second.isMissed)
    }

    @Test
    fun `daily task on its first day has nothing to evaluate yet`() {
        val anchor = LocalDate.of(2026, 3, 10)
        val result = MissedTaskEvaluator.evaluate(
            RepeatType.DAILY, anchor, lastCompletedAt = null, lastMissEvaluatedDate = null,
            today = LocalDate.of(2026, 3, 10), zoneId = zone
        )
        assertFalse(result.isMissed)
    }

    @Test
    fun `catching up after several missed days only counts the most recent elapsed period`() {
        val anchor = LocalDate.of(2026, 3, 1)
        // Jump forward 5 days without ever completing or evaluating.
        val result = MissedTaskEvaluator.evaluate(
            RepeatType.DAILY, anchor, lastCompletedAt = null, lastMissEvaluatedDate = null,
            today = LocalDate.of(2026, 3, 6), zoneId = zone
        )
        assertTrue(result.isMissed)
        assertEquals(LocalDate.of(2026, 3, 5), result.newLastMissEvaluatedDate)
    }
}
