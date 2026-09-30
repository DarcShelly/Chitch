package com.chitch.app.ui.habit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chitch.app.data.entity.HabitStatus
import com.chitch.app.ui.components.FullWidthCta
import com.chitch.app.ui.components.ImageBackdrop
import com.chitch.app.ui.components.StatusBadge

@Composable
fun HabitsListScreen(
    viewModel: HabitsListViewModel,
    searchQuery: String,
    onCreateHabit: () -> Unit,
    onEditHabit: (Long) -> Unit
) {
    val allItems by viewModel.items.collectAsState()
    val reactivateError by viewModel.reactivateError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val query = searchQuery.trim()
    val filtered = if (query.isBlank()) {
        allItems
    } else {
        allItems.filter { it.habit.title.contains(query, ignoreCase = true) }
    }

    LaunchedEffect(reactivateError) {
        reactivateError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissReactivateError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Habits", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        when {
            allItems.isEmpty() -> Column {
                Text(
                    "No habits yet — pick one thing you want to build consistency on.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                FullWidthCta(label = "Add a Habit", onClick = onCreateHabit, icon = Icons.Outlined.SelfImprovement)
            }
            filtered.isEmpty() -> Text(
                "No habits match \"$searchQuery\".",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered, key = { it.habit.id }) { item ->
                    HabitDetailCard(
                        item = item,
                        onReactivate = { viewModel.reactivate(item.habit.id) },
                        onEdit = { onEditHabit(item.habit.id) }
                    )
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState)
    }
}

@Composable
private fun HabitDetailCard(item: HabitListItem, onReactivate: () -> Unit, onEdit: () -> Unit) {
    val habit = item.habit
    val hasImage = habit.imageUri != null
    val contentColor = if (hasImage) Color.White else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (hasImage) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = if (hasImage) {
            CardDefaults.cardColors(containerColor = Color.Transparent)
        } else {
            CardDefaults.cardColors()
        },
        modifier = Modifier
            .fillMaxWidth()
            .then(if (hasImage) Modifier.height(180.dp) else Modifier)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            habit.imageUri?.let { ImageBackdrop(it) }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.SelfImprovement, contentDescription = null, tint = contentColor)
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                    StatusBadge(
                        text = when (habit.status) {
                            HabitStatus.ACTIVE -> "Active"
                            HabitStatus.DRAFT -> "Draft"
                            HabitStatus.COMPLETED -> "Completed"
                            HabitStatus.ARCHIVED -> "Archived"
                        },
                        color = when (habit.status) {
                            HabitStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                            HabitStatus.DRAFT -> MaterialTheme.colorScheme.secondaryContainer
                            HabitStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                            HabitStatus.ARCHIVED -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.ModeEdit, contentDescription = "Edit ${habit.title}", tint = contentColor)
                    }
                }
                habit.description?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = mutedColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                val taskLabel = "${item.taskCount} task${if (item.taskCount == 1) "" else "s"}"
                val nextLabel = item.nextTask?.let { task ->
                    val time = task.startTime ?: task.dueTime
                    if (time != null) "Next: ${task.title} at $time" else "Next: ${task.title}"
                }
                val streakLabel = if (item.currentStreak > 0) "${item.currentStreak}-day streak" else null
                Text(
                    listOfNotNull(taskLabel, nextLabel, streakLabel).joinToString("  •  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor
                )
                if (habit.fileUris.isNotEmpty()) {
                    Text(
                        "${habit.fileUris.size} file(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                if (habit.status == HabitStatus.COMPLETED || habit.status == HabitStatus.ARCHIVED) {
                    TextButton(onClick = onReactivate, modifier = Modifier.padding(top = 6.dp)) {
                        Text("Reactivate", color = contentColor)
                    }
                }
            }
        }
    }
}
