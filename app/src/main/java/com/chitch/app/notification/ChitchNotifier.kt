package com.chitch.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.chitch.app.data.entity.TaskEntity

const val NOTIFICATION_CHANNEL_ID = "chitch_reminders"

/** Creates the reminders channel and shows individual task reminder notifications. */
object ChitchNotifier {

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Task reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders for your tasks, habits, and routine steps"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun show(context: Context, task: TaskEntity, kind: NotificationKind) {
        val hasPermission = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val time = (if (kind == NotificationKind.START) task.startTime else task.dueTime)?.toString().orEmpty()
        val type = when {
            task.routineId != null -> "routine"
            task.habitId != null -> "habit"
            else -> "task"
        }
        val templateKey = templateKeyFor(type, kind)
        val content = NotificationTemplates.render(context, templateKey, task.title, time, type)

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(task.id, kind), notification)
    }

    private fun templateKeyFor(type: String, kind: NotificationKind): NotificationTemplateKey = when (type) {
        "routine" -> if (kind == NotificationKind.START) NotificationTemplateKey.ROUTINE_TASK_START else NotificationTemplateKey.ROUTINE_TASK_DUE
        "habit" -> if (kind == NotificationKind.START) NotificationTemplateKey.HABIT_START else NotificationTemplateKey.HABIT_DUE
        else -> if (kind == NotificationKind.START) NotificationTemplateKey.TASK_START else NotificationTemplateKey.TASK_DUE
    }

    fun notificationId(taskId: Long, kind: NotificationKind): Int = requestCode(taskId, kind)
}

enum class NotificationKind { START, DUE }

fun requestCode(taskId: Long, kind: NotificationKind): Int =
    (taskId.toInt() * 2) + if (kind == NotificationKind.DUE) 1 else 0
