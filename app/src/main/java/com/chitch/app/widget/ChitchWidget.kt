package com.chitch.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.chitch.app.MainActivity
import com.chitch.app.data.AppDatabase
import com.chitch.app.data.entity.HabitEntity
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.domain.isCompletedNow
import java.time.LocalDate
import java.time.LocalTime

private const val MAX_HABITS_SHOWN = 4
private const val MAX_TASKS_SHOWN = 4

private val WidgetBackground = ColorProvider(0xFFF4EFE6.toInt())
private val WidgetTextPrimary = ColorProvider(0xFF3A342C.toInt())
private val WidgetTextMuted = ColorProvider(0xFF8A8377.toInt())

/**
 * Home-screen widget showing today's habit checklist and soon-to-be-pending tasks. Built with
 * Glance rather than classic RemoteViews/XML layouts to match the rest of the app's Compose
 * stack. Data is read directly from Room since the widget renders in the app's own process.
 */
class ChitchWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val today = LocalDate.now()
        val habits = db.habitDao().getActive()
        val checkedHabitIds = db.dailyHabitCheckDao().getCheckedHabitIds(today).toSet()
        val upcomingTasks = db.taskDao().getAllActiveTasks()
            .filterNot { it.isCompletedNow() }
            .sortedBy { it.startTime ?: it.dueTime ?: LocalTime.MAX }
            .take(MAX_TASKS_SHOWN)

        provideContent {
            ChitchWidgetContent(habits, checkedHabitIds, upcomingTasks)
        }
    }
}

@Composable
private fun ChitchWidgetContent(
    habits: List<HabitEntity>,
    checkedHabitIds: Set<Long>,
    upcomingTasks: List<TaskEntity>
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBackground)
            .padding(12.dp)
    ) {
        Text(
            "Chitch",
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = WidgetTextPrimary),
            modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>())
        )
        Spacer(GlanceModifier.height(8.dp))

        Text(
            "Habits",
            style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = WidgetTextPrimary)
        )
        if (habits.isEmpty()) {
            Text("No active habits", style = TextStyle(fontSize = 12.sp, color = WidgetTextMuted))
        } else {
            habits.take(MAX_HABITS_SHOWN).forEach { habit ->
                val done = habit.id in checkedHabitIds
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable(
                            actionRunCallback<ToggleHabitCheckAction>(actionParametersOf(habitIdKey to habit.id))
                        ),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        if (done) "✓" else "○",
                        style = TextStyle(fontSize = 13.sp, color = WidgetTextPrimary),
                        modifier = GlanceModifier.width(18.dp)
                    )
                    Text(
                        habit.title,
                        style = TextStyle(fontSize = 13.sp, color = WidgetTextPrimary),
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(GlanceModifier.height(10.dp))

        Text(
            "Upcoming",
            style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, color = WidgetTextPrimary)
        )
        if (upcomingTasks.isEmpty()) {
            Text("Nothing pending", style = TextStyle(fontSize = 12.sp, color = WidgetTextMuted))
        } else {
            upcomingTasks.forEach { task ->
                val timeLabel = (task.startTime ?: task.dueTime)?.toString() ?: "--"
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable(
                            actionRunCallback<CompleteTaskAction>(actionParametersOf(taskIdKey to task.id))
                        ),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        timeLabel,
                        style = TextStyle(fontSize = 12.sp, color = WidgetTextMuted),
                        modifier = GlanceModifier.width(48.dp)
                    )
                    Text(
                        task.title,
                        style = TextStyle(fontSize = 13.sp, color = WidgetTextPrimary),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
