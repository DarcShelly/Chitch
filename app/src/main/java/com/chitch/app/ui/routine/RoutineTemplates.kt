package com.chitch.app.ui.routine

import com.chitch.app.data.entity.RepeatType
import java.time.LocalTime

data class RoutineTemplateTask(
    val title: String,
    val startTime: LocalTime,
    val dueTime: LocalTime,
    val repeatType: RepeatType = RepeatType.DAILY
)

data class RoutineTemplate(val title: String, val description: String, val tasks: List<RoutineTemplateTask>)

/** Starter routines offered on the create screen — still fully editable after applying one. */
val ROUTINE_TEMPLATES = listOf(
    RoutineTemplate(
        title = "Morning Routine",
        description = "A simple, steady start to the day.",
        tasks = listOf(
            RoutineTemplateTask("Stretch", LocalTime.of(6, 0), LocalTime.of(6, 15)),
            RoutineTemplateTask("Meditate", LocalTime.of(6, 15), LocalTime.of(6, 30)),
            RoutineTemplateTask("Breakfast", LocalTime.of(6, 30), LocalTime.of(7, 0))
        )
    ),
    RoutineTemplate(
        title = "Evening Wind-down",
        description = "Unwind and get ready for bed.",
        tasks = listOf(
            RoutineTemplateTask("Tidy up", LocalTime.of(21, 0), LocalTime.of(21, 15)),
            RoutineTemplateTask("Read", LocalTime.of(21, 15), LocalTime.of(21, 45))
        )
    ),
    RoutineTemplate(
        title = "Workout Day",
        description = "Warm up, train, cool down.",
        tasks = listOf(
            RoutineTemplateTask("Warm up", LocalTime.of(17, 0), LocalTime.of(17, 10)),
            RoutineTemplateTask("Workout", LocalTime.of(17, 10), LocalTime.of(18, 0)),
            RoutineTemplateTask("Stretch & cool down", LocalTime.of(18, 0), LocalTime.of(18, 15))
        )
    )
)
