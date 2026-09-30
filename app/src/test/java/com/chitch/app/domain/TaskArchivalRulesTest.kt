package com.chitch.app.domain

import com.chitch.app.data.entity.HabitStatus
import com.chitch.app.data.entity.RoutineStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskArchivalRulesTest {

    @Test
    fun `standalone task is never archived by this rule`() {
        assertFalse(
            TaskArchivalRules.shouldArchive(
                hasRoutine = false, routineStatus = null, removedFromRoutine = false,
                hasHabit = false, habitStatus = null
            )
        )
    }

    @Test
    fun `routine-only task archives when routine is archived`() {
        assertTrue(
            TaskArchivalRules.shouldArchive(
                hasRoutine = true, routineStatus = RoutineStatus.ARCHIVED, removedFromRoutine = false,
                hasHabit = false, habitStatus = null
            )
        )
    }

    @Test
    fun `routine-only task archives when removed from routine even if routine still active`() {
        assertTrue(
            TaskArchivalRules.shouldArchive(
                hasRoutine = true, routineStatus = RoutineStatus.ACTIVE, removedFromRoutine = true,
                hasHabit = false, habitStatus = null
            )
        )
    }

    @Test
    fun `routine-only task stays alive while routine is active`() {
        assertFalse(
            TaskArchivalRules.shouldArchive(
                hasRoutine = true, routineStatus = RoutineStatus.ACTIVE, removedFromRoutine = false,
                hasHabit = false, habitStatus = null
            )
        )
    }

    @Test
    fun `habit-only task archives when habit is completed`() {
        assertTrue(
            TaskArchivalRules.shouldArchive(
                hasRoutine = false, routineStatus = null, removedFromRoutine = false,
                hasHabit = true, habitStatus = HabitStatus.COMPLETED
            )
        )
    }

    @Test
    fun `habit-only task stays alive while habit is active`() {
        assertFalse(
            TaskArchivalRules.shouldArchive(
                hasRoutine = false, routineStatus = null, removedFromRoutine = false,
                hasHabit = true, habitStatus = HabitStatus.ACTIVE
            )
        )
    }

    @Test
    fun `shared task requires BOTH sides done before archiving - routine still active blocks it`() {
        assertFalse(
            TaskArchivalRules.shouldArchive(
                hasRoutine = true, routineStatus = RoutineStatus.ACTIVE, removedFromRoutine = false,
                hasHabit = true, habitStatus = HabitStatus.COMPLETED
            )
        )
    }

    @Test
    fun `shared task requires BOTH sides done before archiving - habit still active blocks it`() {
        assertFalse(
            TaskArchivalRules.shouldArchive(
                hasRoutine = true, routineStatus = RoutineStatus.ARCHIVED, removedFromRoutine = false,
                hasHabit = true, habitStatus = HabitStatus.ACTIVE
            )
        )
    }

    @Test
    fun `shared task archives once both sides are done`() {
        assertTrue(
            TaskArchivalRules.shouldArchive(
                hasRoutine = true, routineStatus = RoutineStatus.ARCHIVED, removedFromRoutine = false,
                hasHabit = true, habitStatus = HabitStatus.COMPLETED
            )
        )
    }
}
