package com.chitch.app.domain

import com.chitch.app.data.entity.RepeatType
import com.chitch.app.data.entity.TaskEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class MissEvaluation(val isMissed: Boolean, val newLastMissEvaluatedDate: LocalDate?)

/**
 * Detects whether a task's most recently elapsed period ended without completion — exactly
 * once per period. [lastMissEvaluatedDate] is the watermark that stops the same lapsed period
 * from being counted again on every evaluation pass (this runs opportunistically, e.g. once
 * per app open, rather than on a background schedule — see clarification in chat).
 */
object MissedTaskEvaluator {

    fun evaluate(
        repeatType: RepeatType,
        anchorDate: LocalDate,
        lastCompletedAt: Instant?,
        lastMissEvaluatedDate: LocalDate?,
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
        selectedDaysOfWeek: Set<DayOfWeek> = emptySet()
    ): MissEvaluation {
        if (repeatType == RepeatType.ONE_TIME) {
            if (lastMissEvaluatedDate != null) return MissEvaluation(false, lastMissEvaluatedDate)
            if (!today.isAfter(anchorDate)) return MissEvaluation(false, null)
            return MissEvaluation(isMissed = lastCompletedAt == null, newLastMissEvaluatedDate = anchorDate)
        }

        val currentPeriodStart = TaskRecurrence.currentPeriodStart(repeatType, anchorDate, today, selectedDaysOfWeek)
        if (!currentPeriodStart.isAfter(anchorDate)) {
            // Still in (or before) the very first period ever — nothing elapsed to evaluate yet.
            return MissEvaluation(false, lastMissEvaluatedDate)
        }
        val previousPeriodStart =
            TaskRecurrence.currentPeriodStart(repeatType, anchorDate, currentPeriodStart.minusDays(1), selectedDaysOfWeek)

        if (lastMissEvaluatedDate != null && !lastMissEvaluatedDate.isBefore(previousPeriodStart)) {
            return MissEvaluation(false, lastMissEvaluatedDate)
        }

        val completedDate = lastCompletedAt?.atZone(zoneId)?.toLocalDate()
        val completedDuringPreviousPeriod = completedDate != null &&
            !completedDate.isBefore(previousPeriodStart) &&
            completedDate.isBefore(currentPeriodStart)

        return MissEvaluation(
            isMissed = !completedDuringPreviousPeriod,
            newLastMissEvaluatedDate = previousPeriodStart
        )
    }
}

/** Convenience adapter, mirroring [isCompletedNow] — evaluates a real [TaskEntity]. */
fun TaskEntity.evaluateMiss(
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): MissEvaluation {
    val anchor = scheduledDate ?: createdAt.atZone(zoneId).toLocalDate()
    return MissedTaskEvaluator.evaluate(
        repeatType, anchor, lastCompletedAt, lastMissEvaluatedDate, today, zoneId, selectedDaysOfWeek
    )
}
