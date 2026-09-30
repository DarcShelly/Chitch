package com.chitch.app.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.chitch.app.data.entity.TaskLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskLedgerDao {
    @Upsert
    suspend fun upsert(ledger: TaskLedgerEntity)

    @Query("SELECT * FROM task_ledger WHERE id = ${TaskLedgerEntity.SINGLETON_ID} LIMIT 1")
    fun observe(): Flow<TaskLedgerEntity?>

    @Query("SELECT * FROM task_ledger WHERE id = ${TaskLedgerEntity.SINGLETON_ID} LIMIT 1")
    suspend fun get(): TaskLedgerEntity?

    @Query(
        "UPDATE task_ledger SET standaloneTasksCompleted = standaloneTasksCompleted + 1 WHERE id = ${TaskLedgerEntity.SINGLETON_ID}"
    )
    suspend fun incrementStandaloneTasksCompleted()

    @Query(
        "UPDATE task_ledger SET routineTasksCompleted = routineTasksCompleted + 1 WHERE id = ${TaskLedgerEntity.SINGLETON_ID}"
    )
    suspend fun incrementRoutineTasksCompleted()

    @Query(
        "UPDATE task_ledger SET habitTasksCompleted = habitTasksCompleted + 1 WHERE id = ${TaskLedgerEntity.SINGLETON_ID}"
    )
    suspend fun incrementHabitTasksCompleted()

    @Query("UPDATE task_ledger SET habitsCompleted = habitsCompleted + 1 WHERE id = ${TaskLedgerEntity.SINGLETON_ID}")
    suspend fun incrementHabitsCompleted()

    @Query("UPDATE task_ledger SET habitsAbandoned = habitsAbandoned + 1 WHERE id = ${TaskLedgerEntity.SINGLETON_ID}")
    suspend fun incrementHabitsAbandoned()

    @Query("UPDATE task_ledger SET tasksMissed = tasksMissed + 1 WHERE id = ${TaskLedgerEntity.SINGLETON_ID}")
    suspend fun incrementTasksMissed()

    @Query("UPDATE task_ledger SET routineEditCount = routineEditCount + 1 WHERE id = ${TaskLedgerEntity.SINGLETON_ID}")
    suspend fun incrementRoutineEditCount()

    @Query(
        "UPDATE task_ledger SET standaloneTasksCompleted = MAX(0, standaloneTasksCompleted - 1) WHERE id = ${TaskLedgerEntity.SINGLETON_ID}"
    )
    suspend fun decrementStandaloneTasksCompleted()

    @Query(
        "UPDATE task_ledger SET routineTasksCompleted = MAX(0, routineTasksCompleted - 1) WHERE id = ${TaskLedgerEntity.SINGLETON_ID}"
    )
    suspend fun decrementRoutineTasksCompleted()

    @Query(
        "UPDATE task_ledger SET habitTasksCompleted = MAX(0, habitTasksCompleted - 1) WHERE id = ${TaskLedgerEntity.SINGLETON_ID}"
    )
    suspend fun decrementHabitTasksCompleted()
}
