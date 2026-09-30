package com.chitch.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.domain.NotificationScheduling
import java.time.LocalDateTime
import java.time.ZoneId

const val ACTION_TASK_NOTIFICATION = "com.chitch.app.ACTION_TASK_NOTIFICATION"
const val EXTRA_TASK_ID = "task_id"
const val EXTRA_KIND = "kind"

/**
 * Arms/disarms the AlarmManager alarms behind a task's start/due reminders. Scheduling is
 * idempotent (same requestCode -> same PendingIntent gets replaced), so callers can just
 * re-schedule a task after any edit rather than diffing old vs new state.
 */
class NotificationScheduler(private val context: Context) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(AlarmManager::class.java)

    private val zone: ZoneId = ZoneId.systemDefault()

    /** (Re-)arms both the start and due alarms for [task], first clearing any existing ones. */
    fun scheduleForTask(task: TaskEntity) {
        cancelForTask(task.id)
        if (!task.notify) return

        val anchor = task.scheduledDate ?: task.createdAt.atZone(zone).toLocalDate()
        val now = LocalDateTime.now(zone)

        task.startTime?.let { time ->
            NotificationScheduling.nextOccurrence(task.repeatType, anchor, time, now)
                ?.let { scheduleAlarm(task.id, NotificationKind.START, it) }
        }
        task.dueTime?.let { time ->
            NotificationScheduling.nextOccurrence(task.repeatType, anchor, time, now)
                ?.let { scheduleAlarm(task.id, NotificationKind.DUE, it) }
        }
    }

    /** Re-arms only the [kind] alarm for [task] with its next future occurrence — used right after it fires. */
    fun rescheduleAfterFiring(task: TaskEntity, kind: NotificationKind) {
        if (!task.notify) return
        val time = (if (kind == NotificationKind.START) task.startTime else task.dueTime) ?: return
        val anchor = task.scheduledDate ?: task.createdAt.atZone(zone).toLocalDate()
        NotificationScheduling.nextOccurrence(task.repeatType, anchor, time, LocalDateTime.now(zone))
            ?.let { scheduleAlarm(task.id, kind, it) }
    }

    fun cancelForTask(taskId: Long) {
        cancelAlarm(taskId, NotificationKind.START)
        cancelAlarm(taskId, NotificationKind.DUE)
    }

    /** Re-arms every currently-eligible task — used on boot, since alarms don't survive a reboot. */
    fun resyncAll(tasks: List<TaskEntity>) {
        tasks.forEach { scheduleForTask(it) }
    }

    private fun scheduleAlarm(taskId: Long, kind: NotificationKind, triggerAt: LocalDateTime) {
        val triggerMillis = triggerAt.atZone(zone).toInstant().toEpochMilli()
        val pendingIntent = pendingIntentFor(taskId, kind, create = true) ?: return
        val canBeExact = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
        if (canBeExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(taskId: Long, kind: NotificationKind) {
        val pendingIntent = pendingIntentFor(taskId, kind, create = false) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun pendingIntentFor(taskId: Long, kind: NotificationKind, create: Boolean): PendingIntent? {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_TASK_NOTIFICATION
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_KIND, kind.name)
        }
        val flags = (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE) or
            PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode(taskId, kind), intent, flags)
    }
}
