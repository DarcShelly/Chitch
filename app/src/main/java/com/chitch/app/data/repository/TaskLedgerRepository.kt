package com.chitch.app.data.repository

import com.chitch.app.data.dao.TaskLedgerDao
import com.chitch.app.data.entity.TaskLedgerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskLedgerRepository(private val dao: TaskLedgerDao) {

    fun observe(): Flow<TaskLedgerEntity> = dao.observe().map { it ?: TaskLedgerEntity() }

    suspend fun ensureRowExists() {
        if (dao.get() == null) dao.upsert(TaskLedgerEntity())
    }

    suspend fun recordStandaloneTaskCompleted() {
        ensureRowExists()
        dao.incrementStandaloneTasksCompleted()
    }

    suspend fun recordRoutineTaskCompleted() {
        ensureRowExists()
        dao.incrementRoutineTasksCompleted()
    }

    suspend fun recordHabitTaskCompleted() {
        ensureRowExists()
        dao.incrementHabitTasksCompleted()
    }

    suspend fun recordHabitCompleted() {
        ensureRowExists()
        dao.incrementHabitsCompleted()
    }

    suspend fun recordHabitAbandoned() {
        ensureRowExists()
        dao.incrementHabitsAbandoned()
    }

    suspend fun recordTaskMissed() {
        ensureRowExists()
        dao.incrementTasksMissed()
    }

    suspend fun recordRoutineEdited() {
        ensureRowExists()
        dao.incrementRoutineEditCount()
    }

    /** Reverses one of [recordStandaloneTaskCompleted]/[recordRoutineTaskCompleted]/[recordHabitTaskCompleted]. */
    suspend fun undoTaskCompleted(routineId: Long?, habitId: Long?) {
        ensureRowExists()
        when {
            routineId != null -> dao.decrementRoutineTasksCompleted()
            habitId != null -> dao.decrementHabitTasksCompleted()
            else -> dao.decrementStandaloneTasksCompleted()
        }
    }
}
