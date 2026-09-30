package com.chitch.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.chitch.app.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AlarmManager alarms are cleared on reboot — re-arm everything currently eligible. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = AppDatabase.getInstance(context).taskDao().getEffectiveActiveNotifiableTasks()
                NotificationScheduler(context).resyncAll(tasks)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
