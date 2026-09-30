package com.chitch.app.data.repository

import androidx.room.withTransaction
import com.chitch.app.data.AppDatabase
import com.chitch.app.data.entity.HabitEntity
import com.chitch.app.data.entity.HabitStatus
import com.chitch.app.data.entity.RepeatType
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.entity.TaskStatus
import com.chitch.app.domain.ScheduleValidator
import com.chitch.app.domain.TaskArchivalRules
import com.chitch.app.domain.TimeWindow
import com.chitch.app.notification.NotificationScheduler
import com.chitch.app.widget.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

const val MAX_ACTIVE_HABITS = 3

data class HabitTaskDraft(
    val title: String,
    val startTime: LocalTime? = null,
    val dueTime: LocalTime? = null,
    val notify: Boolean = true,
    val repeatType: RepeatType = RepeatType.DAILY,
    val scheduledDate: LocalDate? = null
)

sealed class HabitCreationResult {
    data class Success(val habitId: Long) : HabitCreationResult()
    data class InternalCollision(val a: HabitTaskDraft, val b: HabitTaskDraft) : HabitCreationResult()
    data class ExternalCollision(val with: TaskEntity) : HabitCreationResult()
    data object TooManyActiveHabits : HabitCreationResult()
}

class HabitRepository(
    private val db: AppDatabase,
    private val ledgerRepository: TaskLedgerRepository,
    private val notificationScheduler: NotificationScheduler,
    private val widgetRefresher: WidgetRefresher
) {

    private val habitDao = db.habitDao()
    private val taskDao = db.taskDao()
    private val routineDao = db.routineDao()

    fun observeActive(): Flow<List<HabitEntity>> = habitDao.observeActive()

    fun observeAll(): Flow<List<HabitEntity>> = habitDao.observeAll()

    suspend fun getById(habitId: Long): HabitEntity? = habitDao.getById(habitId)

    /** Metadata-only edit (title/description/attachments) — task list is managed separately. */
    suspend fun updateHabit(
        habitId: Long,
        title: String,
        description: String?,
        imageUri: String?,
        fileUris: List<String>
    ) {
        val existing = habitDao.getById(habitId) ?: return
        habitDao.update(
            existing.copy(
                title = title,
                description = description,
                imageUri = imageUri,
                fileUris = fileUris,
                updatedAt = Instant.now()
            )
        )
    }

    suspend fun createHabit(
        title: String,
        description: String?,
        imageUri: String?,
        fileUris: List<String>,
        activateNow: Boolean,
        taskDrafts: List<HabitTaskDraft>
    ): HabitCreationResult {
        // Habit tasks may or may not have times — only the timed ones participate in collisions.
        val timedDrafts = taskDrafts.filter { it.startTime != null && it.dueTime != null }
        val windows = timedDrafts.map { TimeWindow(it.startTime!!, it.dueTime!!) }

        ScheduleValidator.findInternalCollision(windows)?.let { (w1, w2) ->
            val a = timedDrafts[windows.indexOf(w1)]
            val b = timedDrafts[windows.indexOf(w2)]
            return HabitCreationResult.InternalCollision(a, b)
        }

        val existingTimed = taskDao.getEffectiveActiveTimedTasks()
        for (window in windows) {
            val collision = existingTimed.firstOrNull {
                TimeWindow(it.startTime!!, it.dueTime!!).overlaps(window)
            }
            if (collision != null) return HabitCreationResult.ExternalCollision(collision)
        }

        if (activateNow && habitDao.getActiveCount() >= MAX_ACTIVE_HABITS) {
            return HabitCreationResult.TooManyActiveHabits
        }

        val habitId = db.withTransaction {
            val now = Instant.now()
            val id = habitDao.insert(
                HabitEntity(
                    title = title,
                    description = description,
                    imageUri = imageUri,
                    fileUris = fileUris,
                    status = if (activateNow) HabitStatus.ACTIVE else HabitStatus.DRAFT,
                    streakWindowStartDate = LocalDate.now(),
                    createdAt = now,
                    updatedAt = now
                )
            )
            taskDrafts.forEach { draft ->
                val taskEntity = TaskEntity(
                    title = draft.title,
                    habitId = id,
                    startTime = draft.startTime,
                    dueTime = draft.dueTime,
                    repeatType = draft.repeatType,
                    scheduledDate = draft.scheduledDate,
                    notify = draft.notify,
                    createdAt = now,
                    updatedAt = now
                )
                val taskId = taskDao.insert(taskEntity)
                if (activateNow) notificationScheduler.scheduleForTask(taskEntity.copy(id = taskId))
            }
            id
        }

        widgetRefresher.refresh()
        return HabitCreationResult.Success(habitId)
    }

    /** Attaches an existing task to this habit, subject to the same collision check as creation. */
    suspend fun assignTask(taskId: Long, habitId: Long): TaskAssignResult {
        val task = taskDao.getById(taskId) ?: return TaskAssignResult.Success
        if (task.startTime != null && task.dueTime != null) {
            val candidate = TimeWindow(task.startTime, task.dueTime)
            val collision = taskDao.getEffectiveActiveTimedTasks(excludingId = taskId).firstOrNull {
                TimeWindow(it.startTime!!, it.dueTime!!).overlaps(candidate)
            }
            if (collision != null) return TaskAssignResult.Collision(collision)
        }
        val nextSortOrder = (taskDao.getMaxHabitSortOrder(habitId) ?: -1) + 1
        val updated = task.copy(habitId = habitId, sortOrder = nextSortOrder, updatedAt = Instant.now())
        taskDao.update(updated)
        notificationScheduler.scheduleForTask(updated)
        widgetRefresher.refresh()
        return TaskAssignResult.Success
    }

    /** Detaches a task from its habit, archiving it if that was its only remaining container. */
    suspend fun removeTaskFromHabit(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        if (task.habitId == null) return
        val routineStatus = task.routineId?.let { routineDao.getById(it)?.status }
        val archivable = TaskArchivalRules.shouldArchive(
            hasRoutine = task.routineId != null,
            routineStatus = routineStatus,
            removedFromRoutine = false,
            hasHabit = true,
            habitStatus = null
        )
        db.withTransaction {
            taskDao.update(
                task.copy(
                    habitId = null,
                    status = if (archivable) TaskStatus.ARCHIVED else task.status,
                    updatedAt = Instant.now()
                )
            )
        }
        if (archivable) notificationScheduler.cancelForTask(task.id)
        widgetRefresher.refresh()
    }

    /**
     * Returns false if 3 habits are already active (see the max-active-habits rule). Also used
     * to reactivate a COMPLETED or ARCHIVED habit: restarts its 30-day streak window and revives
     * any tasks that were archived when the habit left ACTIVE status.
     */
    suspend fun activateHabit(habitId: Long): Boolean {
        if (habitDao.getActiveCount(excludingId = habitId) >= MAX_ACTIVE_HABITS) return false
        habitDao.setStatus(habitId, HabitStatus.ACTIVE)
        habitDao.resetStreakWindow(habitId, LocalDate.now())
        val now = Instant.now()
        taskDao.getByHabit(habitId).forEach { task ->
            val revived = if (task.status == TaskStatus.ARCHIVED) {
                task.copy(status = TaskStatus.ACTIVE, updatedAt = now).also { taskDao.update(it) }
            } else {
                task
            }
            notificationScheduler.scheduleForTask(revived)
        }
        widgetRefresher.refresh()
        return true
    }

    suspend fun completeHabit(habitId: Long) {
        val alreadyCompleted = habitDao.getById(habitId)?.status == HabitStatus.COMPLETED
        db.withTransaction {
            habitDao.setStatus(habitId, HabitStatus.COMPLETED)
            archiveEligibleTasks(taskDao.getByHabit(habitId), HabitStatus.COMPLETED)
        }
        if (!alreadyCompleted) ledgerRepository.recordHabitCompleted()
        widgetRefresher.refresh()
    }

    /**
     * Assumption #3: archiving a habit also archives its tasks, same as routines.
     * Counts as "abandoned" in the ledger unless the habit was already completed —
     * archiving a habit you finished isn't abandoning it.
     */
    suspend fun archiveHabit(habitId: Long) {
        val wasCompleted = habitDao.getById(habitId)?.status == HabitStatus.COMPLETED
        db.withTransaction {
            habitDao.setStatus(habitId, HabitStatus.ARCHIVED)
            archiveEligibleTasks(taskDao.getByHabit(habitId), HabitStatus.ARCHIVED)
        }
        if (!wasCompleted) ledgerRepository.recordHabitAbandoned()
        widgetRefresher.refresh()
    }

    private suspend fun archiveEligibleTasks(tasks: List<TaskEntity>, habitStatus: HabitStatus) {
        val now = Instant.now()
        tasks.forEach { task ->
            val routineStatus = task.routineId?.let { routineDao.getById(it)?.status }
            val archivable = TaskArchivalRules.shouldArchive(
                hasRoutine = task.routineId != null,
                routineStatus = routineStatus,
                removedFromRoutine = false,
                hasHabit = true,
                habitStatus = habitStatus
            )
            if (archivable && task.status != TaskStatus.ARCHIVED) {
                taskDao.update(task.copy(status = TaskStatus.ARCHIVED, updatedAt = now))
                notificationScheduler.cancelForTask(task.id)
            }
        }
    }
}
