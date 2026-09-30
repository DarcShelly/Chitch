package com.chitch.app.data.repository

import androidx.room.withTransaction
import com.chitch.app.data.AppDatabase
import com.chitch.app.data.entity.RepeatType
import com.chitch.app.data.entity.RoutineEntity
import com.chitch.app.data.entity.RoutineStatus
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.entity.TaskStatus
import com.chitch.app.data.entity.UserEntity
import com.chitch.app.domain.HoursCheckResult
import com.chitch.app.domain.ScheduleValidator
import com.chitch.app.domain.TaskArchivalRules
import com.chitch.app.domain.TimeWindow
import com.chitch.app.notification.NotificationScheduler
import com.chitch.app.widget.WidgetRefresher
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

data class RoutineTaskDraft(
    val title: String,
    val startTime: LocalTime,
    val dueTime: LocalTime,
    val notify: Boolean = true,
    val repeatType: RepeatType = RepeatType.DAILY,
    val scheduledDate: LocalDate? = null
)

sealed class RoutineCreationResult {
    data class Success(val routineId: Long) : RoutineCreationResult()
    data class InternalCollision(val a: RoutineTaskDraft, val b: RoutineTaskDraft) : RoutineCreationResult()
    data class HoursExceeded(val spanHours: Double, val maxAllowedHours: Double) : RoutineCreationResult()
    data class ExternalCollision(val with: TaskEntity) : RoutineCreationResult()
    data object AnotherRoutineActive : RoutineCreationResult()
}

sealed class TaskAssignResult {
    data object Success : TaskAssignResult()
    data class Collision(val with: TaskEntity) : TaskAssignResult()
}

class RoutineRepository(
    private val db: AppDatabase,
    private val ledgerRepository: TaskLedgerRepository,
    private val notificationScheduler: NotificationScheduler,
    private val widgetRefresher: WidgetRefresher
) {

    private val routineDao = db.routineDao()
    private val taskDao = db.taskDao()
    private val userDao = db.userDao()
    private val habitDao = db.habitDao()

    fun observeAll(): Flow<List<RoutineEntity>> = routineDao.observeAll()

    fun observeActive(): Flow<RoutineEntity?> = routineDao.observeActive()

    suspend fun getById(routineId: Long): RoutineEntity? = routineDao.getById(routineId)

    /**
     * Metadata-only edit (title/description/attachments) — the routine's task list isn't
     * editable yet (see the "no edit-routine UI yet" scope note in chat). Counts toward the
     * ledger's routine-edit tally.
     */
    suspend fun updateRoutine(
        routineId: Long,
        title: String,
        description: String?,
        imageUri: String?,
        fileUris: List<String>
    ) {
        val existing = routineDao.getById(routineId) ?: return
        routineDao.update(
            existing.copy(
                title = title,
                description = description,
                imageUri = imageUri,
                fileUris = fileUris,
                updatedAt = Instant.now()
            )
        )
        ledgerRepository.recordRoutineEdited()
    }

    suspend fun createRoutine(
        title: String,
        description: String?,
        imageUri: String? = null,
        fileUris: List<String> = emptyList(),
        activateNow: Boolean,
        taskDrafts: List<RoutineTaskDraft>
    ): RoutineCreationResult {
        val windows = taskDrafts.map { TimeWindow(it.startTime, it.dueTime) }

        ScheduleValidator.findInternalCollision(windows)?.let { (w1, w2) ->
            val a = taskDrafts[windows.indexOf(w1)]
            val b = taskDrafts[windows.indexOf(w2)]
            return RoutineCreationResult.InternalCollision(a, b)
        }

        val user = userDao.get() ?: UserEntity()
        val hours = ScheduleValidator.validateRoutineHours(windows, user.sleepHours, user.workStart, user.workEnd)
        if (hours is HoursCheckResult.Exceeded) {
            return RoutineCreationResult.HoursExceeded(hours.spanHours, hours.maxAllowedHours)
        }

        val existingTimed = taskDao.getEffectiveActiveTimedTasks()
        for (window in windows) {
            val collision = existingTimed.firstOrNull {
                TimeWindow(it.startTime!!, it.dueTime!!).overlaps(window)
            }
            if (collision != null) return RoutineCreationResult.ExternalCollision(collision)
        }

        if (activateNow && routineDao.getActive() != null) {
            return RoutineCreationResult.AnotherRoutineActive
        }

        val routineId = db.withTransaction {
            val now = Instant.now()
            val id = routineDao.insert(
                RoutineEntity(
                    title = title,
                    description = description,
                    imageUri = imageUri,
                    fileUris = fileUris,
                    status = if (activateNow) RoutineStatus.ACTIVE else RoutineStatus.DRAFT,
                    createdAt = now,
                    updatedAt = now
                )
            )
            taskDrafts.forEach { draft ->
                val taskEntity = TaskEntity(
                    title = draft.title,
                    routineId = id,
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
        return RoutineCreationResult.Success(routineId)
    }

    /** Returns false if a different routine is already active (see clarification #6). */
    suspend fun activateRoutine(routineId: Long): Boolean {
        val currentActive = routineDao.getActive()
        if (currentActive != null && currentActive.id != routineId) return false
        routineDao.setStatus(routineId, RoutineStatus.ACTIVE)
        taskDao.getByRoutine(routineId).forEach { notificationScheduler.scheduleForTask(it) }
        widgetRefresher.refresh()
        return true
    }

    /** Archives the routine, then cascade-archives its tasks per [TaskArchivalRules] (assumption #3). */
    suspend fun archiveRoutine(routineId: Long) {
        db.withTransaction {
            routineDao.setStatus(routineId, RoutineStatus.ARCHIVED)
            archiveEligibleTasks(taskDao.getByRoutine(routineId), routineArchived = true, removedFromRoutine = false)
        }
        widgetRefresher.refresh()
    }

    /** Attaches an existing task to this routine, subject to the same collision check as creation. */
    suspend fun assignTask(taskId: Long, routineId: Long): TaskAssignResult {
        val task = taskDao.getById(taskId) ?: return TaskAssignResult.Success
        if (task.startTime != null && task.dueTime != null) {
            val candidate = TimeWindow(task.startTime, task.dueTime)
            val collision = taskDao.getEffectiveActiveTimedTasks(excludingId = taskId).firstOrNull {
                TimeWindow(it.startTime!!, it.dueTime!!).overlaps(candidate)
            }
            if (collision != null) return TaskAssignResult.Collision(collision)
        }
        val nextSortOrder = (taskDao.getMaxRoutineSortOrder(routineId) ?: -1) + 1
        val updated = task.copy(routineId = routineId, sortOrder = nextSortOrder, updatedAt = Instant.now())
        taskDao.update(updated)
        notificationScheduler.scheduleForTask(updated)
        widgetRefresher.refresh()
        return TaskAssignResult.Success
    }

    /** Detaches a task from its routine, archiving it if that was its only remaining container. */
    suspend fun removeTaskFromRoutine(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        if (task.routineId == null) return
        val habitStatus = task.habitId?.let { habitDao.getById(it)?.status }
        val archivable = TaskArchivalRules.shouldArchive(
            hasRoutine = true,
            routineStatus = null,
            removedFromRoutine = true,
            hasHabit = task.habitId != null,
            habitStatus = habitStatus
        )
        db.withTransaction {
            taskDao.update(
                task.copy(
                    routineId = null,
                    status = if (archivable) TaskStatus.ARCHIVED else task.status,
                    updatedAt = Instant.now()
                )
            )
        }
        if (archivable) notificationScheduler.cancelForTask(task.id)
        widgetRefresher.refresh()
    }

    private suspend fun archiveEligibleTasks(
        tasks: List<TaskEntity>,
        routineArchived: Boolean,
        removedFromRoutine: Boolean
    ) {
        val now = Instant.now()
        tasks.forEach { task ->
            val habitStatus = task.habitId?.let { habitDao.getById(it)?.status }
            val archivable = TaskArchivalRules.shouldArchive(
                hasRoutine = task.routineId != null,
                routineStatus = if (routineArchived) RoutineStatus.ARCHIVED else null,
                removedFromRoutine = removedFromRoutine,
                hasHabit = task.habitId != null,
                habitStatus = habitStatus
            )
            if (archivable && task.status != TaskStatus.ARCHIVED) {
                taskDao.update(task.copy(status = TaskStatus.ARCHIVED, updatedAt = now))
                notificationScheduler.cancelForTask(task.id)
            }
        }
    }
}
