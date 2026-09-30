package com.chitch.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.chitch.app.ChitchApplication

val taskIdKey = ActionParameters.Key<Long>("taskId")

/**
 * Marks a task complete from the widget's "Upcoming" row without opening the app. Glance
 * instantiates callbacks by reflection (no-arg constructor), so app state is reached through
 * [ChitchApplication]'s container rather than constructor injection.
 */
class CompleteTaskAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[taskIdKey] ?: return
        val container = (context.applicationContext as ChitchApplication).container
        container.taskRepository.completeTask(taskId)
        ChitchWidget().updateAll(context)
    }
}
