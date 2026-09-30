package com.chitch.app.data.entity

/**
 * Recurrence rule for a task. ONE_TIME stands in for the spec's "null" repeat value —
 * modeled as an explicit enum value so the Room column stays non-null.
 */
enum class RepeatType {
    ONE_TIME,
    DAILY,
    ALTERNATE,
    WEEKLY,
    MONTHLY
}

/**
 * Container-level lifecycle status of a task. Per-occurrence completion for recurring
 * tasks is tracked separately via [TaskEntity.lastCompletedAt] (see [com.chitch.app.domain.TaskRecurrence]),
 * not here — so there's no COMPLETED value: a task is either still live (ACTIVE) or
 * permanently done (ARCHIVED).
 */
enum class TaskStatus {
    ACTIVE,
    ARCHIVED
}

/** Lifecycle status of a routine. Only one routine may be ACTIVE at a time. */
enum class RoutineStatus {
    ACTIVE,
    DRAFT,
    ARCHIVED
}

/** Lifecycle status of a habit. Only three habits may be ACTIVE at a time. */
enum class HabitStatus {
    ACTIVE,
    DRAFT,
    COMPLETED,
    ARCHIVED
}

/** Sad-to-happy self-rating for a day's [DailyReviewEntity]. Ordinal order matters for averaging. */
enum class MoodRating {
    VERY_SAD,
    SAD,
    NEUTRAL,
    HAPPY,
    VERY_HAPPY
}
