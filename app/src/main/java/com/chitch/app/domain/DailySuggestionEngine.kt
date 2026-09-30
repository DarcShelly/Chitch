package com.chitch.app.domain

import com.chitch.app.data.entity.MoodRating
import java.time.LocalDate

data class DailySuggestionInput(
    val date: LocalDate,
    val mood: MoodRating,
    val tasksMissedCount: Int,
    val habitsMissedCount: Int,
    val hasThingsMissedNote: Boolean
)

/**
 * Small rule-based, offline suggestion generator — no network calls or LLM involved, this is a
 * fully local/self-contained app. Deterministic given the same input (the "generic tip" rotates
 * by day-of-year rather than randomly, so it's both varied day-to-day and unit-testable).
 */
object DailySuggestionEngine {

    private val GENERIC_TIPS = listOf(
        "Prep tomorrow's top task tonight so you can start the day already moving.",
        "Try a 5-minute walk before your next task — it resets focus better than a phone break.",
        "Write down one thing you're looking forward to tomorrow.",
        "Pick one habit to do first thing tomorrow, before anything else competes for it.",
        "Tidy your workspace for two minutes — small resets compound."
    )

    fun suggest(input: DailySuggestionInput, maxSuggestions: Int = 3): List<String> {
        val suggestions = mutableListOf<String>()

        if (input.mood.ordinal <= MoodRating.SAD.ordinal) {
            suggestions += "Today was tough — consider a short walk, some music, or reaching out to a friend tonight."
        }
        if (input.tasksMissedCount > 0) {
            val plural = if (input.tasksMissedCount == 1) "" else "s"
            suggestions += "You missed ${input.tasksMissedCount} task$plural today — " +
                "consider rescheduling them or trimming tomorrow's list."
        }
        if (input.habitsMissedCount > 0) {
            suggestions += "Missed a habit today? Tomorrow's a fresh shot — even a tiny version of it still counts."
        }
        if (input.hasThingsMissedNote) {
            suggestions += "Since something didn't go well today, try turning it into one small, concrete change for tomorrow."
        }
        if (suggestions.isEmpty() && input.mood.ordinal >= MoodRating.HAPPY.ordinal &&
            input.tasksMissedCount == 0 && input.habitsMissedCount == 0
        ) {
            suggestions += "Great day! Keep the momentum by prepping tomorrow's top task tonight."
        }

        if (suggestions.size < maxSuggestions) {
            val tipIndex = input.date.dayOfYear % GENERIC_TIPS.size
            val tip = GENERIC_TIPS[tipIndex]
            if (tip !in suggestions) suggestions += tip
        }

        return suggestions.take(maxSuggestions)
    }
}
