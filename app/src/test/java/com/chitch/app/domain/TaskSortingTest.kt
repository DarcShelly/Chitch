package com.chitch.app.domain

import com.chitch.app.data.entity.RepeatType
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.entity.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

class TaskSortingTest {

    private fun task(
        id: Long,
        startTime: LocalTime? = null,
        dueTime: LocalTime? = null,
        status: TaskStatus = TaskStatus.ACTIVE,
        lastCompletedAt: Instant? = null,
        repeatType: RepeatType = RepeatType.DAILY
    ) = TaskEntity(
        id = id,
        title = "task$id",
        startTime = startTime,
        dueTime = dueTime,
        status = status,
        lastCompletedAt = lastCompletedAt,
        repeatType = repeatType,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH
    )

    @Test
    fun `open tasks sort before completed and archived, by soonest time`() {
        val late = task(1, startTime = LocalTime.of(18, 0))
        val early = task(2, startTime = LocalTime.of(6, 0))
        val completed = task(3, startTime = LocalTime.of(1, 0), lastCompletedAt = Instant.now())
        val archived = task(4, startTime = LocalTime.of(0, 0), status = TaskStatus.ARCHIVED)

        val sorted = listOf(late, completed, archived, early).sortedForDisplay()

        assertEquals(listOf(early.id, late.id, completed.id, archived.id), sorted.map { it.id })
    }

    @Test
    fun `tasks without a time sort after timed tasks within the same group`() {
        val timed = task(1, startTime = LocalTime.of(9, 0))
        val untimed = task(2)

        val sorted = listOf(untimed, timed).sortedForDisplay()

        assertEquals(listOf(timed.id, untimed.id), sorted.map { it.id })
    }
}
