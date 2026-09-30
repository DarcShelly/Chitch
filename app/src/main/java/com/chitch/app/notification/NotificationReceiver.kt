package com.chitch.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chitch.app.data.AppDatabase
import com.chitch.app.data.entity.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires when a task's start/due alarm goes off: shows the notification, then re-arms the next one. */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TASK_NOTIFICATION) return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val kind = intent.getStringExtra(EXTRA_KIND)?.let { runCatching { NotificationKind.valueOf(it) }.getOrNull() }
        if (taskId == -1L || kind == null) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = AppDatabase.getInstance(context).taskDao().getById(taskId)
                if (task != null && task.status == TaskStatus.ACTIVE && task.notify) {
                    ChitchNotifier.show(context, task, kind)
                    NotificationScheduler(context).rescheduleAfterFiring(task, kind)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
