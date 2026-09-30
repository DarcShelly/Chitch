package com.chitch.app.domain

import com.chitch.app.data.entity.HabitStatus
import com.chitch.app.data.entity.RoutineStatus

/**
 * Whether a routine/habit task should be permanently archived, given the state of its
 * container(s). Standalone tasks (no routineId, no habitId) are never archived by this —
 * their archival happens directly at completion time (see TaskRepository.completeTask).
 *
 * Rules (see clarifications in chat):
 *  - routine-only task: archived once the routine is archived, or the task is removed from it.
 *  - habit-only task: archived once the habit is completed (or archived — assumption #3).
 *  - shared task (both set): archived only once BOTH sides are done — a task still tied to
 *    a live routine stays alive even if its habit finished, and vice versa.
 */
object TaskArchivalRules {

    fun shouldArchive(
        hasRoutine: Boolean,
        routineStatus: RoutineStatus?,
        removedFromRoutine: Boolean,
        hasHabit: Boolean,
        habitStatus: HabitStatus?
    ): Boolean {
        if (!hasRoutine && !hasHabit) return false

        val routineSideDone = !hasRoutine ||
            removedFromRoutine ||
            routineStatus == RoutineStatus.ARCHIVED

        val habitSideDone = hasHabit &&
            (habitStatus == HabitStatus.COMPLETED || habitStatus == HabitStatus.ARCHIVED)

        return when {
            hasRoutine && hasHabit -> routineSideDone && habitSideDone
            hasRoutine -> routineSideDone
            else -> habitSideDone
        }
    }
}
