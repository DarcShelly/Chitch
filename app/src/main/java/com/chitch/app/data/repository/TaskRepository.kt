package com.chitch.app.data.repository

import com.chitch.app.data.dao.TaskDao
import com.chitch.app.data.entity.RepeatType
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.entity.TaskStatus
import com.chitch.app.domain.TimeWindow
import com.chitch.app.domain.evaluateMiss
import com.chitch.app.domain.isCompletedNow
import com.chitch.app.notification.NotificationScheduler
import com.chitch.app.widget.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

sealed class TaskCreationResult {
    data class Success(val id: Long) : TaskCreationResult()
    data class Collision(val with: TaskEntity) : TaskCreationResult()
}

sealed class TaskUpdateResult {
    data object Success : TaskUpdateResult()
    data class Collision(val with: TaskEntity) : TaskUpdateResult()
}

class TaskRepository(
    private val taskDao: TaskDao,
    private val ledgerRepository: TaskLedgerRepository,
    private val notificationScheduler: NotificationScheduler,
    private val widgetRefresher: WidgetRefresher
) {

    fun observeActiveStandalone(): Flow<List<TaskEntity>> = taskDao.observeStandalone()

    /** Every currently-active task regardless of container — used by the daily review checklist. */
    fun observeAllActiveTasks(): Flow<List<TaskEntity>> = taskDao.observeAllActiveTasks()

    fun observeAllStandalone(): Flow<List<TaskEntity>> = taskDao.observeAllStandalone()

    /** Every task regardless of container — backs the "add existing task" search picker. */
    fun observeAllTasksRaw(): Flow<List<TaskEntity>> = taskDao.observeAllTasksRaw()

    fun observeByRoutine(routineId: Long): Flow<List<TaskEntity>> = taskDao.observeByRoutine(routineId)

    fun observeByHabit(habitId: Long): Flow<List<TaskEntity>> = taskDao.observeByHabit(habitId)

    suspend fun getById(taskId: Long): TaskEntity? = taskDao.getById(taskId)

    /** Inserts [draft], rejecting it if its time window collides with another effectively-active task. */
    suspend fun createTask(draft: TaskEntity): TaskCreationResult {
        if (draft.startTime != null && draft.dueTime != null) {
            val candidate = TimeWindow(draft.startTime, draft.dueTime)
            val excludingId = if (draft.id != 0L) draft.id else -1
            val collision = taskDao.getEffectiveActiveTimedTasks(excludingId).firstOrNull { existing ->
                TimeWindow(existing.startTime!!, existing.dueTime!!).overlaps(candidate) &&
                    daysConflict(existing.selectedDaysOfWeek, draft.selectedDaysOfWeek)
            }
            if (collision != null) return TaskCreationResult.Collision(collision)
        }

        val now = Instant.now()
        // A task created directly into a routine/habit (e.g. via "+ Add new task") appends to the
        // end of that container's list rather than sorting first at the default sortOrder of 0.
        val sortOrder = when {
            draft.routineId != null -> (taskDao.getMaxRoutineSortOrder(draft.routineId) ?: -1) + 1
            draft.habitId != null -> (taskDao.getMaxHabitSortOrder(draft.habitId) ?: -1) + 1
            else -> draft.sortOrder
        }
        val toInsert = draft.copy(sortOrder = sortOrder, createdAt = now, updatedAt = now)
        val id = taskDao.insert(toInsert)
        notificationScheduler.scheduleForTask(toInsert.copy(id = id))
        widgetRefresher.refresh()
        return TaskCreationResult.Success(id)
    }

    /** Updates an existing task's editable fields, rejecting a time-window collision like [createTask]. */
    suspend fun updateTask(
        taskId: Long,
        title: String,
        description: String?,
        imageUri: String?,
        fileUris: List<String>,
        startTime: LocalTime?,
        dueTime: LocalTime?,
        repeatType: RepeatType,
        scheduledDate: LocalDate?,
        notify: Boolean,
        selectedDaysOfWeek: Set<DayOfWeek> = emptySet()
    ): TaskUpdateResult {
        val existing = taskDao.getById(taskId) ?: return TaskUpdateResult.Success
        if (startTime != null && dueTime != null) {
            val candidate = TimeWindow(startTime, dueTime)
            val collision = taskDao.getEffectiveActiveTimedTasks(excludingId = taskId).firstOrNull {
                TimeWindow(it.startTime!!, it.dueTime!!).overlaps(candidate) &&
                    daysConflict(it.selectedDaysOfWeek, selectedDaysOfWeek)
            }
            if (collision != null) return TaskUpdateResult.Collision(collision)
        }
        val updated = existing.copy(
            title = title,
            description = description,
            imageUri = imageUri,
            fileUris = fileUris,
            startTime = startTime,
            dueTime = dueTime,
            repeatType = repeatType,
            scheduledDate = scheduledDate,
            notify = notify,
            selectedDaysOfWeek = if (repeatType == RepeatType.WEEKLY) selectedDaysOfWeek else emptySet(),
            updatedAt = Instant.now()
        )
        taskDao.update(updated)
        notificationScheduler.scheduleForTask(updated)
        widgetRefresher.refresh()
        return TaskUpdateResult.Success
    }

    /**
     * Two timed tasks only truly collide if they can land on the same calendar day. When both
     * sides opt into day-of-week mode (non-empty set), only flag it if the day-sets intersect.
     * If either side isn't using day-of-week mode, fall back to the conservative "always check"
     * behavior every other repeat type already relies on.
     */
    private fun daysConflict(a: Set<DayOfWeek>, b: Set<DayOfWeek>): Boolean {
        if (a.isEmpty() || b.isEmpty()) return true
        return a.intersect(b).isNotEmpty()
    }

    /** Manually archives a task without marking it done (distinct from [completeTask]'s auto-archive). */
    suspend fun archiveTask(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        if (task.status == TaskStatus.ARCHIVED) return
        taskDao.update(task.copy(status = TaskStatus.ARCHIVED, updatedAt = Instant.now()))
        notificationScheduler.cancelForTask(taskId)
        widgetRefresher.refresh()
    }

    /** Restores an archived task back to active. */
    suspend fun unarchiveTask(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        if (task.status != TaskStatus.ARCHIVED) return
        val updated = task.copy(status = TaskStatus.ACTIVE, updatedAt = Instant.now())
        taskDao.update(updated)
        notificationScheduler.scheduleForTask(updated)
        widgetRefresher.refresh()
    }

    /**
     * Marks a task done. One-time standalone tasks archive immediately (clarification #7);
     * everything else (recurring standalone, routine/habit tasks) just stamps
     * [TaskEntity.lastCompletedAt] — [com.chitch.app.domain.TaskRecurrence] derives whether
     * that still counts as "completed" for the current period. The ledger only counts a
     * completion once per period — re-tapping an already-completed task doesn't double count.
     */
    suspend fun completeTask(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        val alreadyCompletedThisPeriod = task.isCompletedNow()
        val now = Instant.now()
        val isStandaloneOneTime = task.routineId == null && task.habitId == null &&
            task.repeatType == RepeatType.ONE_TIME
        val updated = if (isStandaloneOneTime) {
            task.copy(status = TaskStatus.ARCHIVED, lastCompletedAt = now, updatedAt = now)
        } else {
            task.copy(lastCompletedAt = now, updatedAt = now)
        }
        taskDao.update(updated)
        if (isStandaloneOneTime) notificationScheduler.cancelForTask(task.id)

        if (!alreadyCompletedThisPeriod) {
            when {
                task.routineId != null -> ledgerRepository.recordRoutineTaskCompleted()
                task.habitId != null -> ledgerRepository.recordHabitTaskCompleted()
                else -> ledgerRepository.recordStandaloneTaskCompleted()
            }
        }
        widgetRefresher.refresh()
    }

    /** Reverses [completeTask] for the current period (no-op if already archived). */
    suspend fun uncompleteTask(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        if (task.status == TaskStatus.ARCHIVED) return
        taskDao.update(task.copy(lastCompletedAt = null, updatedAt = Instant.now()))
        widgetRefresher.refresh()
    }

    /**
     * Reverses a swipe-triggered [completeTask]/[archiveTask] within the same "session" (the
     * snackbar's undo window). Restores [snapshot] verbatim rather than calling the opposite
     * function by name, since [completeTask] auto-archives one-time standalone tasks and neither
     * [uncompleteTask] nor [archiveTask]/[unarchiveTask] alone can reverse that combination.
     * [wasNewCompletion] must be the caller's `!task.isCompletedNow()` reading from *before* the
     * original action, so the ledger counter it bumped is decremented exactly when it was bumped.
     */
    suspend fun undoAction(snapshot: TaskEntity, wasNewCompletion: Boolean) {
        taskDao.update(snapshot)
        if (snapshot.status == TaskStatus.ACTIVE) {
            notificationScheduler.scheduleForTask(snapshot)
        } else {
            notificationScheduler.cancelForTask(snapshot.id)
        }
        if (wasNewCompletion) {
            ledgerRepository.undoTaskCompleted(snapshot.routineId, snapshot.habitId)
        }
        widgetRefresher.refresh()
    }

    /**
     * Opportunistic pass (run once per app open — see clarification in chat) that checks every
     * active task's most recently elapsed period and records a ledger miss the first time it
     * finds one that ended without completion.
     */
    suspend fun evaluateAndRecordMisses() {
        taskDao.getAllActiveTasks().forEach { task ->
            val result = task.evaluateMiss()
            if (result.newLastMissEvaluatedDate != task.lastMissEvaluatedDate) {
                taskDao.update(task.copy(lastMissEvaluatedDate = result.newLastMissEvaluatedDate))
            }
            if (result.isMissed) {
                ledgerRepository.recordTaskMissed()
            }
        }
    }

    /** Persists a drag-and-drop reorder within a routine/habit's task list. */
    suspend fun reorderTasks(orderedTaskIds: List<Long>) {
        val now = Instant.now()
        val reordered = orderedTaskIds.mapIndexedNotNull { index, taskId ->
            taskDao.getById(taskId)?.copy(sortOrder = index, updatedAt = now)
        }
        taskDao.updateAll(reordered)
    }

    /**
     * Re-arms every eligible task's alarms (run once per app open, alongside
     * [evaluateAndRecordMisses]) — a safety net alongside per-mutation scheduling, since it's
     * the only thing that catches a device reboot that happened while the app never opened.
     */
    suspend fun resyncNotifications() {
        notificationScheduler.resyncAll(taskDao.getEffectiveActiveNotifiableTasks())
    }
}
