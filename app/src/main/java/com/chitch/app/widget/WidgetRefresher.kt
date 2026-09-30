package com.chitch.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

/**
 * Triggers an immediate widget redraw after a mutation that changes what it shows (a task's
 * done state, the active habit list, today's habit checks, etc). The system's own
 * updatePeriodMillis refresh (30 min minimum) is too slow to feel live, so repositories call
 * this right after the write that matters instead of waiting for it.
 */
class WidgetRefresher(private val context: Context) {
    suspend fun refresh() {
        ChitchWidget().updateAll(context)
    }
}
