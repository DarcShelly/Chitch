package com.chitch.app.domain

import com.chitch.app.data.entity.MoodRating
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DailySuggestionEngineTest {

    private val date = LocalDate.of(2026, 3, 10)

    @Test
    fun `low mood triggers a self-care suggestion`() {
        val input = DailySuggestionInput(date, MoodRating.SAD, tasksMissedCount = 0, habitsMissedCount = 0, hasThingsMissedNote = false)
        val suggestions = DailySuggestionEngine.suggest(input)
        assertTrue(suggestions.any { it.contains("tough", ignoreCase = true) })
    }

    @Test
    fun `missed tasks triggers a rescheduling suggestion`() {
        val input = DailySuggestionInput(date, MoodRating.NEUTRAL, tasksMissedCount = 2, habitsMissedCount = 0, hasThingsMissedNote = false)
        val suggestions = DailySuggestionEngine.suggest(input)
        assertTrue(suggestions.any { it.contains("missed 2 tasks", ignoreCase = true) })
    }

    @Test
    fun `missed habit triggers an encouragement suggestion`() {
        val input = DailySuggestionInput(date, MoodRating.NEUTRAL, tasksMissedCount = 0, habitsMissedCount = 1, hasThingsMissedNote = false)
        val suggestions = DailySuggestionEngine.suggest(input)
        assertTrue(suggestions.any { it.contains("habit", ignoreCase = true) })
    }

    @Test
    fun `great day with nothing missed gets a celebratory suggestion`() {
        val input = DailySuggestionInput(date, MoodRating.VERY_HAPPY, tasksMissedCount = 0, habitsMissedCount = 0, hasThingsMissedNote = false)
        val suggestions = DailySuggestionEngine.suggest(input)
        assertTrue(suggestions.any { it.contains("Great day", ignoreCase = true) })
    }

    @Test
    fun `always returns at least one suggestion and respects the max`() {
        val input = DailySuggestionInput(date, MoodRating.SAD, tasksMissedCount = 3, habitsMissedCount = 2, hasThingsMissedNote = true)
        val suggestions = DailySuggestionEngine.suggest(input, maxSuggestions = 2)
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.size <= 2)
    }

    @Test
    fun `same input on the same date is deterministic`() {
        val input = DailySuggestionInput(date, MoodRating.HAPPY, tasksMissedCount = 0, habitsMissedCount = 0, hasThingsMissedNote = false)
        assertTrue(DailySuggestionEngine.suggest(input) == DailySuggestionEngine.suggest(input))
    }
}
