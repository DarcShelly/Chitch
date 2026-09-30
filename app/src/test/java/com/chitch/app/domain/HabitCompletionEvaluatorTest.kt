package com.chitch.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitCompletionEvaluatorTest {

    @Test
    fun `not eligible before window has fully elapsed`() {
        val start = LocalDate.of(2026, 1, 1)
        val today = LocalDate.of(2026, 1, 20)
        val done = (0 until 20).map { start.plusDays(it.toLong()) }.toSet()
        val result = HabitCompletionEvaluator.evaluate(start, today, done, maxMissedDays = 3)
        assertFalse(result.eligible)
        assertEquals(20, result.daysElapsed)
    }

    @Test
    fun `perfect 30-day streak is eligible with zero misses`() {
        val start = LocalDate.of(2026, 1, 1)
        val today = start.plusDays(29)
        val done = (0..29).map { start.plusDays(it.toLong()) }.toSet()
        val result = HabitCompletionEvaluator.evaluate(start, today, done, maxMissedDays = 3)
        assertTrue(result.eligible)
        assertEquals(30, result.daysElapsed)
        assertEquals(0, result.missedDaysInWindow)
    }

    @Test
    fun `misses within the allowance still complete`() {
        val start = LocalDate.of(2026, 1, 1)
        val today = start.plusDays(29)
        val done = (0..29).filter { it != 5 && it != 10 }.map { start.plusDays(it.toLong()) }.toSet()
        val result = HabitCompletionEvaluator.evaluate(start, today, done, maxMissedDays = 3)
        assertTrue(result.eligible)
        assertEquals(2, result.missedDaysInWindow)
    }

    @Test
    fun `misses beyond the allowance are not eligible`() {
        val start = LocalDate.of(2026, 1, 1)
        val today = start.plusDays(29)
        val done = (0..29).filter { it % 5 != 0 }.map { start.plusDays(it.toLong()) }.toSet()
        val result = HabitCompletionEvaluator.evaluate(start, today, done, maxMissedDays = 3)
        assertFalse(result.eligible)
        assertEquals(6, result.missedDaysInWindow)
    }

    @Test
    fun `only the most recent window days are evaluated once habit is older than the window`() {
        val start = LocalDate.of(2026, 1, 1)
        val today = start.plusDays(59)
        // First 30 days all missed, but they're outside the rolling 30-day window ending today.
        val done = (30..59).map { start.plusDays(it.toLong()) }.toSet()
        val result = HabitCompletionEvaluator.evaluate(start, today, done, maxMissedDays = 0)
        assertTrue(result.eligible)
        assertEquals(0, result.missedDaysInWindow)
        assertEquals(60, result.daysElapsed)
    }

    @Test
    fun `zero max missed days requires a perfect recent window`() {
        val start = LocalDate.of(2026, 1, 1)
        val today = start.plusDays(29)
        val done = (0..28).map { start.plusDays(it.toLong()) }.toSet()
        val result = HabitCompletionEvaluator.evaluate(start, today, done, maxMissedDays = 0)
        assertFalse(result.eligible)
        assertEquals(1, result.missedDaysInWindow)
    }
}
