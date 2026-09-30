package com.chitch.app.domain

import java.time.LocalDate

/** Consecutive-day streak from a set of "done" dates — distinct from [HabitCompletionEvaluator]'s
 * misses-tolerant eligibility window; this is a strict, unforgiving count. */
object StreakCalculator {
    /**
     * Counts backward from [today] while each day is present in [doneDates]. If [today] itself
     * isn't done yet, starts from yesterday instead — so the streak doesn't drop to zero before
     * the user has had a chance to check in today.
     */
    fun currentStreak(doneDates: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in doneDates) today else today.minusDays(1)
        var streak = 0
        while (cursor in doneDates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
