package com.chitch.app.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Decides whether a habit has been "built" — done consistently enough over a rolling 30-day
 * window, with a configurable number of misses tolerated (see [com.chitch.app.data.entity.UserEntity.maxMissedDaysForHabitCompletion]).
 * Pure and Android-free so it's unit-testable like the rest of the domain layer.
 */
object HabitCompletionEvaluator {

    const val DEFAULT_WINDOW_DAYS = 30

    data class Result(
        val eligible: Boolean,
        val daysElapsed: Int,
        val missedDaysInWindow: Int,
        val windowDays: Int
    )

    /**
     * [windowStartDate] anchors the habit's current streak attempt (reset on every activation).
     * [doneDates] are the calendar days, anywhere on or after [windowStartDate], the habit was
     * checked off. Not eligible until the habit has existed for at least [windowDays] days;
     * once it has, only the most recent [windowDays] days are evaluated for misses.
     */
    fun evaluate(
        windowStartDate: LocalDate,
        today: LocalDate,
        doneDates: Set<LocalDate>,
        maxMissedDays: Int,
        windowDays: Int = DEFAULT_WINDOW_DAYS
    ): Result {
        val daysElapsed = (ChronoUnit.DAYS.between(windowStartDate, today) + 1).toInt()
        if (daysElapsed < windowDays) {
            return Result(eligible = false, daysElapsed = daysElapsed, missedDaysInWindow = 0, windowDays = windowDays)
        }

        val recentWindowStart = today.minusDays((windowDays - 1).toLong())
        var missed = 0
        var day = recentWindowStart
        while (!day.isAfter(today)) {
            if (day !in doneDates) missed++
            day = day.plusDays(1)
        }

        return Result(
            eligible = missed <= maxMissedDays,
            daysElapsed = daysElapsed,
            missedDaysInWindow = missed,
            windowDays = windowDays
        )
    }
}
