package com.chitch.app.ui.habit

data class HabitTemplateTask(val title: String)
data class HabitTemplate(val title: String, val description: String, val tasks: List<HabitTemplateTask>)

/** Starter habits offered on the create screen — still fully editable after applying one. */
val HABIT_TEMPLATES = listOf(
    HabitTemplate(
        title = "Meditate",
        description = "A few quiet minutes, once a day.",
        tasks = listOf(HabitTemplateTask("Meditate"))
    ),
    HabitTemplate(
        title = "Drink Water",
        description = "Stay hydrated through the day.",
        tasks = listOf(HabitTemplateTask("Drink a glass of water"))
    ),
    HabitTemplate(
        title = "Read",
        description = "A little reading, every day.",
        tasks = listOf(HabitTemplateTask("Read"))
    )
)
