package com.chitch.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.domain.isCompletedNow
import com.chitch.app.ui.components.ActiveItemCard
import com.chitch.app.ui.components.EmptyStateCtas
import com.chitch.app.ui.components.FullWidthCta
import com.chitch.app.ui.components.repeatTypeLabel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    searchQuery: String,
    onCreateTask: () -> Unit,
    onCreateRoutine: () -> Unit,
    onCreateHabit: () -> Unit,
    onEditHabit: (Long) -> Unit,
    onEditRoutine: (Long) -> Unit,
    onEditTask: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Search only filters what's shown here — it never changes the layout/order.
    val query = searchQuery.trim()
    val habitItems = if (query.isBlank()) {
        state.habitItems
    } else {
        state.habitItems.filter { it.habit.title.contains(query, ignoreCase = true) }
    }
    val routineItem = state.routineItem?.takeIf { query.isBlank() || it.routine.title.contains(query, ignoreCase = true) }
    val taskItems = if (query.isBlank()) {
        state.taskItems
    } else {
        state.taskItems.filter { it.task.title.contains(query, ignoreCase = true) }
    }
    val hasActiveRoutineOrTasks = routineItem != null || taskItems.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Habits sit at the very top of the page, above routines/tasks, with a
        // persistent full-width CTA (see the Home layout spec in chat).
        FullWidthCta(label = "Create Habit", onClick = onCreateHabit)

        if (habitItems.isEmpty() && !hasActiveRoutineOrTasks && query.isBlank()) {
            Text(
                "Nothing set up yet — start with a habit, a routine, or a one-off task.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (habitItems.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                habitItems.forEach { item ->
                    ActiveItemCard(
                        title = item.habit.title,
                        subtitle = taskSummaryLabel(item.taskCount, item.nextTask),
                        icon = Icons.Outlined.SelfImprovement,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        imageUri = item.habit.imageUri,
                        onClick = { onEditHabit(item.habit.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Text(
            text = "Active now",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
        )

        if (hasActiveRoutineOrTasks) {
            routineItem?.let { item ->
                ActiveItemCard(
                    title = item.routine.title,
                    subtitle = taskSummaryLabel(item.taskCount, item.nextTask),
                    icon = Icons.Outlined.Repeat,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    imageUri = item.routine.imageUri,
                    onClick = { onEditRoutine(item.routine.id) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }

            if (taskItems.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    taskItems.forEach { item ->
                        HomeTaskRow(
                            item = item,
                            onToggleComplete = {
                                if (item.task.isCompletedNow()) {
                                    viewModel.uncompleteTask(item.task.id)
                                } else {
                                    viewModel.completeTask(item.task.id)
                                }
                            },
                            onClick = { onEditTask(item.task.id) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Always reachable, not just the empty state — see the "no way back into
        // Create Task/Routine once something is active" gap flagged in chat.
        EmptyStateCtas(onCreateTask = onCreateTask, onCreateRoutine = onCreateRoutine)
    }
}

private fun taskSummaryLabel(taskCount: Int, nextTask: TaskEntity?): String {
    val countLabel = "$taskCount task${if (taskCount == 1) "" else "s"}"
    val nextLabel = nextTask?.let { task ->
        val time = task.startTime ?: task.dueTime
        if (time != null) "Next: ${task.title} at $time" else "Next: ${task.title}"
    }
    return listOfNotNull(countLabel, nextLabel).joinToString("  •  ")
}

@Composable
private fun HomeTaskRow(
    item: HomeTaskItem,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit
) {
    val task = item.task
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.isCompletedNow()) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = "Toggle complete",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onToggleComplete)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val timeLabel = if (task.startTime != null && task.dueTime != null) {
                    "${task.startTime}–${task.dueTime}"
                } else {
                    repeatTypeLabel(task.repeatType)
                }
                val meta = listOfNotNull(timeLabel, item.parentLabel).joinToString("  •  ")
                Text(
                    meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
