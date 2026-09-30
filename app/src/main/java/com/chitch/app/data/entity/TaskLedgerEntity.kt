package com.chitch.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Aggregate counters only — never the individual tasks/habits/routines themselves,
 * per the spec ("need not contain the specific tasks but the number of them").
 * Singleton row, always id = 1.
 */
@Entity(tableName = "task_ledger")
data class TaskLedgerEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,

    val standaloneTasksCompleted: Int = 0,
    val routineTasksCompleted: Int = 0,
    val habitTasksCompleted: Int = 0,

    val habitsCompleted: Int = 0,
    val habitsAbandoned: Int = 0,

    val tasksMissed: Int = 0,

    val routineEditCount: Int = 0
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
