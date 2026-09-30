package com.chitch.app.domain

import com.chitch.app.data.entity.RepeatType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Pure computation of "when does this task's alarm next fire", reusing the same anchor/period
 * concept as [TaskRecurrence] but looking forward instead of back. Kept Android-free (no
 * AlarmManager/Context) so it's unit-testable like the rest of the domain layer.
 */
object NotificationScheduling {

    /**
     * The next date/time [time] occurs on or after [now], for a task recurring per
     * [repeatType] anchored at [anchorDate]. Returns null for a one-time task whose single
     * occurrence has already passed — there's nothing left to schedule.
     */
    fun nextOccurrence(
        repeatType: RepeatType,
        anchorDate: LocalDate,
        time: LocalTime,
        now: LocalDateTime
    ): LocalDateTime? {
        val today = now.toLocalDate()
        val todayCandidateDate = periodDateOnOrAfter(repeatType, anchorDate, today)
        val todayCandidate = LocalDateTime.of(todayCandidateDate, time)
        if (todayCandidate.isAfter(now)) return todayCandidate

        if (repeatType == RepeatType.ONE_TIME) return null

        val nextDate = periodDateOnOrAfter(repeatType, anchorDate, today.plusDays(1))
        return LocalDateTime.of(nextDate, time)
    }

    private fun periodDateOnOrAfter(repeatType: RepeatType, anchorDate: LocalDate, referenceDate: LocalDate): LocalDate =
        when (repeatType) {
            RepeatType.ONE_TIME -> anchorDate
            RepeatType.DAILY -> referenceDate
            RepeatType.ALTERNATE -> bucketOnOrAfter(anchorDate, referenceDate, periodLengthDays = 2)
            RepeatType.WEEKLY -> bucketOnOrAfter(anchorDate, referenceDate, periodLengthDays = 7)
            RepeatType.MONTHLY -> monthlyOnOrAfter(anchorDate, referenceDate)
        }

    private fun bucketOnOrAfter(anchorDate: LocalDate, referenceDate: LocalDate, periodLengthDays: Long): LocalDate {
        if (referenceDate.isBefore(anchorDate)) return anchorDate
        val daysSinceAnchor = ChronoUnit.DAYS.between(anchorDate, referenceDate)
        val remainder = daysSinceAnchor % periodLengthDays
        return if (remainder == 0L) referenceDate else referenceDate.plusDays(periodLengthDays - remainder)
    }

    private fun monthlyOnOrAfter(anchorDate: LocalDate, referenceDate: LocalDate): LocalDate {
        if (referenceDate.isBefore(anchorDate)) return anchorDate
        val thisMonthOccurrence = referenceDate.withDayOfMonth(
            minOf(anchorDate.dayOfMonth, referenceDate.lengthOfMonth())
        )
        if (!thisMonthOccurrence.isBefore(referenceDate)) return thisMonthOccurrence
        val nextMonth = referenceDate.plusMonths(1)
        return nextMonth.withDayOfMonth(minOf(anchorDate.dayOfMonth, nextMonth.lengthOfMonth()))
    }
}
