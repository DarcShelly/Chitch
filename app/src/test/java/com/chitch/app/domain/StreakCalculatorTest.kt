package com.chitch.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 8, 20)

    @Test
    fun `no done dates is a zero streak`() {
        assertEquals(0, StreakCalculator.currentStreak(emptySet(), today))
    }

    @Test
    fun `counts consecutive days ending today`() {
        val doneDates = setOf(today, today.minusDays(1), today.minusDays(2))
        assertEquals(3, StreakCalculator.currentStreak(doneDates, today))
    }

    @Test
    fun `a gap stops the count`() {
        val doneDates = setOf(today, today.minusDays(1), today.minusDays(3))
        assertEquals(2, StreakCalculator.currentStreak(doneDates, today))
    }

    @Test
    fun `today not yet checked still counts an in-progress streak from yesterday`() {
        val doneDates = setOf(today.minusDays(1), today.minusDays(2))
        assertEquals(2, StreakCalculator.currentStreak(doneDates, today))
    }

    @Test
    fun `neither today nor yesterday done is a zero streak`() {
        val doneDates = setOf(today.minusDays(2))
        assertEquals(0, StreakCalculator.currentStreak(doneDates, today))
    }
}
