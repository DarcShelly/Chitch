package com.chitch.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.entity.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Update
    suspend fun updateAll(tasks: List<TaskEntity>)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    /** Every active task regardless of container — used for the missed-task evaluation pass. */
    @Query("SELECT * FROM tasks WHERE status = 'ACTIVE'")
    suspend fun getAllActiveTasks(): List<TaskEntity>

    /** Reactive version of [getAllActiveTasks] — used for the daily review's task checklist. */
    @Query("SELECT * FROM tasks WHERE status = 'ACTIVE' ORDER BY startTime")
    fun observeAllActiveTasks(): Flow<List<TaskEntity>>

    /** Standalone tasks: no routine, no habit. */
    @Query(
        "SELECT * FROM tasks WHERE status = :status AND routineId IS NULL AND habitId IS NULL ORDER BY startTime"
    )
    fun observeStandalone(status: TaskStatus = TaskStatus.ACTIVE): Flow<List<TaskEntity>>

    /** All standalone tasks regardless of status — for the Tasks tab's full list view. */
    @Query(
        "SELECT * FROM tasks WHERE routineId IS NULL AND habitId IS NULL ORDER BY status, startTime"
    )
    fun observeAllStandalone(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE routineId = :routineId ORDER BY sortOrder, startTime")
    fun observeByRoutine(routineId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE routineId = :routineId")
    suspend fun getByRoutine(routineId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE habitId = :habitId ORDER BY sortOrder, startTime")
    fun observeByHabit(habitId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE habitId = :habitId")
    suspend fun getByHabit(habitId: Long): List<TaskEntity>

    @Query("SELECT MAX(sortOrder) FROM tasks WHERE routineId = :routineId")
    suspend fun getMaxRoutineSortOrder(routineId: Long): Int?

    @Query("SELECT MAX(sortOrder) FROM tasks WHERE habitId = :habitId")
    suspend fun getMaxHabitSortOrder(habitId: Long): Int?

    /** Every task regardless of container or status — backs the "add existing task" search picker. */
    @Query("SELECT * FROM tasks ORDER BY status, title")
    fun observeAllTasksRaw(): Flow<List<TaskEntity>>

    /**
     * Tasks that are "effectively active" right now with a defined time window — used for
     * collision checks. A task counts only if its own status is ACTIVE AND (it has no
     * routine, or that routine is ACTIVE) AND (it has no habit, or that habit is ACTIVE).
     * See clarification #2 in chat.
     */
    @Query(
        """
        SELECT tasks.* FROM tasks
        LEFT JOIN routines ON tasks.routineId = routines.id
        LEFT JOIN habits ON tasks.habitId = habits.id
        WHERE tasks.status = 'ACTIVE'
          AND tasks.startTime IS NOT NULL AND tasks.dueTime IS NOT NULL
          AND (tasks.routineId IS NULL OR routines.status = 'ACTIVE')
          AND (tasks.habitId IS NULL OR habits.status = 'ACTIVE')
          AND tasks.id != :excludingId
        """
    )
    suspend fun getEffectiveActiveTimedTasks(excludingId: Long = -1): List<TaskEntity>

    /**
     * Every task eligible for a scheduled notification right now: active, opted in, has at
     * least one of start/due time, and (if it belongs to a routine/habit) that container is
     * ACTIVE. Used to re-arm alarms after a reboot or app open, since AlarmManager alarms
     * don't survive a reboot and per-mutation scheduling can't cover every edge case.
     */
    @Query(
        """
        SELECT tasks.* FROM tasks
        LEFT JOIN routines ON tasks.routineId = routines.id
        LEFT JOIN habits ON tasks.habitId = habits.id
        WHERE tasks.status = 'ACTIVE'
          AND tasks.notify = 1
          AND (tasks.startTime IS NOT NULL OR tasks.dueTime IS NOT NULL)
          AND (tasks.routineId IS NULL OR routines.status = 'ACTIVE')
          AND (tasks.habitId IS NULL OR habits.status = 'ACTIVE')
        """
    )
    suspend fun getEffectiveActiveNotifiableTasks(): List<TaskEntity>
}
