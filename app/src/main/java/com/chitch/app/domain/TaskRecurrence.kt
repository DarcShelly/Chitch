package com.chitch.app.domain

import com.chitch.app.data.entity.RepeatType
import com.chitch.app.data.entity.TaskEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Computes whether a recurring task is "done for now" from a single [lastCompletedAt]
 * timestamp, instead of an actively-reset background job. Completion is tracked per
 * calendar-day period (bucketed by repeat rule), so marking a task done at any point during
 * today's period — including before its start time — counts for today and only reverts once
 * the next period begins. See clarification #6 in chat: this is anchor-based rolling periods
 * (e.g. every 7 days from creation), not calendar-aligned weeks/months.
 */
object TaskRecurrence {

    /**
     * The first day of the period that contains [today], for a recurring task created on
     * [anchorDate]. When [repeatType] is WEEKLY and [selectedDaysOfWeek] is non-empty, the task
     * is due on exactly those weekdays instead of the legacy rolling 7-day bucket — the "period"
     * becomes the most recent matching weekday on or before [today].
     */
    fun currentPeriodStart(
        repeatType: RepeatType,
        anchorDate: LocalDate,
        today: LocalDate,
        selectedDaysOfWeek: Set<DayOfWeek> = emptySet()
    ): LocalDate =
        when (repeatType) {
            RepeatType.ONE_TIME -> anchorDate
            RepeatType.DAILY -> today
            RepeatType.ALTERNATE -> bucketStart(anchorDate, today, periodLengthDays = 2)
            RepeatType.WEEKLY -> if (selectedDaysOfWeek.isEmpty()) {
                bucketStart(anchorDate, today, periodLengthDays = 7)
            } else {
                mostRecentDueDate(selectedDaysOfWeek, today)
            }
            RepeatType.MONTHLY -> monthlyPeriodStart(anchorDate, today)
        }

    fun isCompletedForCurrentPeriod(
        lastCompletedAt: Instant?,
        repeatType: RepeatType,
        anchorDate: LocalDate,
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
        selectedDaysOfWeek: Set<DayOfWeek> = emptySet()
    ): Boolean {
        if (lastCompletedAt == null) return false
        if (repeatType == RepeatType.ONE_TIME) return true

        val completedDate = lastCompletedAt.atZone(zoneId).toLocalDate()
        val periodStart = currentPeriodStart(repeatType, anchorDate, today, selectedDaysOfWeek)
        return !completedDate.isBefore(periodStart) && !completedDate.isAfter(today)
    }

    private fun bucketStart(anchorDate: LocalDate, today: LocalDate, periodLengthDays: Long): LocalDate {
        val daysSinceAnchor = ChronoUnit.DAYS.between(anchorDate, today)
        val bucketIndex = Math.floorDiv(daysSinceAnchor, periodLengthDays)
        return anchorDate.plusDays(bucketIndex * periodLengthDays)
    }

    /** Walks backward from [today] to the most recent date whose weekday is in [selectedDays]. */
    private fun mostRecentDueDate(selectedDays: Set<DayOfWeek>, today: LocalDate): LocalDate {
        var candidate = today
        while (candidate.dayOfWeek !in selectedDays) {
            candidate = candidate.minusDays(1)
        }
        return candidate
    }

    private fun monthlyPeriodStart(anchorDate: LocalDate, today: LocalDate): LocalDate {
        val thisMonthOccurrence = today.withDayOfMonth(
            minOf(anchorDate.dayOfMonth, today.lengthOfMonth())
        )
        return if (thisMonthOccurrence.isAfter(today)) {
            val prevMonth = today.minusMonths(1)
            prevMonth.withDayOfMonth(minOf(anchorDate.dayOfMonth, prevMonth.lengthOfMonth()))
        } else {
            thisMonthOccurrence
        }
    }
}

/**
 * Convenience adapter from a real [TaskEntity] onto the pure [TaskRecurrence] functions.
 * When a [TaskEntity.scheduledDate] is set — now allowed for routine/habit tasks too, not
 * just standalone ones (clarification #2 in chat) — it anchors the recurrence instead of
 * [TaskEntity.createdAt], since "start this recurring task from a specific date" should win
 * over whenever it happened to be created.
 */
fun TaskEntity.isCompletedNow(zoneId: ZoneId = ZoneId.systemDefault()): Boolean {
    val anchor = scheduledDate ?: createdAt.atZone(zoneId).toLocalDate()
    val today = LocalDate.now(zoneId)
    return TaskRecurrence.isCompletedForCurrentPeriod(lastCompletedAt, repeatType, anchor, today, zoneId, selectedDaysOfWeek)
}
