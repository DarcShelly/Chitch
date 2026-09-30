package com.chitch.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.chitch.app.ChitchApplication
import java.time.LocalDate

val habitIdKey = ActionParameters.Key<Long>("habitId")

/** Toggles today's check-in for a habit from the widget's "Habits" row. See [CompleteTaskAction]. */
class ToggleHabitCheckAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val habitId = parameters[habitIdKey] ?: return
        val container = (context.applicationContext as ChitchApplication).container
        container.dailyHabitCheckRepository.toggleCheck(habitId, LocalDate.now())
        ChitchWidget().updateAll(context)
    }
}
