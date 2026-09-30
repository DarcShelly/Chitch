package com.chitch.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val imageUri: String? = null,
    val fileUris: List<String> = emptyList(),
    val description: String? = null,

    /** Null when this task isn't part of a routine. Non-null tasks must have both times. */
    val routineId: Long? = null,
    /** Null when this task isn't part of a habit. A task may have both set (shared). */
    val habitId: Long? = null,

    val startTime: LocalTime? = null,
    val dueTime: LocalTime? = null,

    /**
     * For one-time tasks, the specific day it's due. For recurring tasks (standalone,
     * routine, or habit — not just standalone anymore, per a later clarification), the date
     * the recurrence pattern anchors to, e.g. "start this weekly task from next Monday"
     * instead of whenever it was created. Optional either way.
     */
    val scheduledDate: LocalDate? = null,
    val repeatType: RepeatType = RepeatType.ONE_TIME,

    /**
     * Only meaningful when [repeatType] is WEEKLY. Empty means "not using day-of-week mode" —
     * the task keeps the legacy rolling 7-day-bucket behavior. Non-empty means the task is due
     * exactly on these weekdays (see [com.chitch.app.domain.TaskRecurrence]).
     */
    val selectedDaysOfWeek: Set<DayOfWeek> = emptySet(),

    val notify: Boolean = true,
    val status: TaskStatus = TaskStatus.ACTIVE,

    /** Manual ordering within a routine/habit's task list. Higher sorts later. Irrelevant for standalone tasks. */
    val sortOrder: Int = 0,

    /**
     * When this task (or its current occurrence, for recurring tasks) was last marked done.
     * See [com.chitch.app.domain.TaskRecurrence] for how this becomes "completed" vs "reset".
     */
    val lastCompletedAt: Instant? = null,

    /**
     * The last period-start date already checked (and, if applicable, counted) for a miss.
     * Prevents [com.chitch.app.domain.MissedTaskEvaluator] from recounting the same lapsed
     * period every time the evaluator runs.
     */
    val lastMissEvaluatedDate: LocalDate? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
