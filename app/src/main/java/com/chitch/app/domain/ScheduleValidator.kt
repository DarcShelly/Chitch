package com.chitch.app.domain

import java.time.Duration
import java.time.LocalTime

/**
 * A same-day [start, end) time window. Windows that only touch at a boundary
 * (one ends exactly when another starts) do not count as colliding.
 */
data class TimeWindow(val start: LocalTime, val end: LocalTime) {
    init {
        require(start < end) {
            "Task time windows must not cross midnight for now (start=$start, end=$end)"
        }
    }

    fun overlaps(other: TimeWindow): Boolean = start < other.end && other.start < end
}

sealed class HoursCheckResult {
    data object Ok : HoursCheckResult()
    data class Exceeded(val spanHours: Double, val maxAllowedHours: Double) : HoursCheckResult()
}

/**
 * Pure, Android-free scheduling rules shared by standalone/habit task creation
 * and routine creation. Kept dependency-free so it runs as a plain JVM unit test.
 */
object ScheduleValidator {

    /** First existing window that collides with [candidate], or null if none. */
    fun findCollision(candidate: TimeWindow, existing: List<TimeWindow>): TimeWindow? =
        existing.firstOrNull { it.overlaps(candidate) }

    /** First colliding pair among [windows] (e.g. tasks within the same routine), or null. */
    fun findInternalCollision(windows: List<TimeWindow>): Pair<TimeWindow, TimeWindow>? {
        for (i in windows.indices) {
            for (j in i + 1 until windows.size) {
                if (windows[i].overlaps(windows[j])) return windows[i] to windows[j]
            }
        }
        return null
    }

    /**
     * A routine's required span (earliest start to latest end) must fit within the
     * hours left in a day once sleep and working hours are subtracted. See
     * clarification #5 — this is a duration check, not a literal time-of-day overlap
     * check against working/sleep windows.
     */
    fun validateRoutineHours(
        windows: List<TimeWindow>,
        sleepHours: Int,
        workStart: LocalTime?,
        workEnd: LocalTime?
    ): HoursCheckResult {
        if (windows.isEmpty()) return HoursCheckResult.Ok

        val earliestStart = windows.minOf { it.start }
        val latestEnd = windows.maxOf { it.end }
        val spanMinutes = Duration.between(earliestStart, latestEnd).toMinutes()

        val workingMinutes = if (workStart != null && workEnd != null) {
            Duration.between(workStart, workEnd).toMinutes().coerceAtLeast(0)
        } else 0L

        val sleepMinutes = sleepHours * 60L
        val maxAllowedMinutes = (24 * 60L) - sleepMinutes - workingMinutes

        return if (spanMinutes > maxAllowedMinutes) {
            HoursCheckResult.Exceeded(
                spanHours = spanMinutes / 60.0,
                maxAllowedHours = maxAllowedMinutes / 60.0
            )
        } else {
            HoursCheckResult.Ok
        }
    }
}
