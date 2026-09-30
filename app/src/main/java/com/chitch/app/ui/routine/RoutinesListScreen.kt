package com.chitch.app.ui.routine

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
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chitch.app.data.entity.RoutineStatus
import com.chitch.app.ui.components.FullWidthCta
import com.chitch.app.ui.components.ImageBackdrop
import com.chitch.app.ui.components.StatusBadge

@Composable
fun RoutinesListScreen(
    viewModel: RoutinesListViewModel,
    searchQuery: String,
    onCreateRoutine: () -> Unit,
    onEditRoutine: (Long) -> Unit
) {
    val allItems by viewModel.items.collectAsState()
    val query = searchQuery.trim()
    val filtered = if (query.isBlank()) {
        allItems
    } else {
        allItems.filter { it.routine.title.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Routines", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        when {
            allItems.isEmpty() -> Column {
                Text(
                    "No routines yet — set up a sequence of timed tasks for part of your day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                FullWidthCta(label = "Add a Routine", onClick = onCreateRoutine, icon = Icons.Outlined.Repeat)
            }
            filtered.isEmpty() -> Text(
                "No routines match \"$searchQuery\".",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filtered, key = { it.routine.id }) { item ->
                    RoutineDetailCard(
                        item = item,
                        onClick = { onEditRoutine(item.routine.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RoutineDetailCard(item: RoutineListItem, onClick: () -> Unit) {
    val routine = item.routine
    val hasImage = routine.imageUri != null
    val contentColor = if (hasImage) Color.White else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (hasImage) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        onClick = onClick,
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
            routine.imageUri?.let { ImageBackdrop(it) }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Repeat, contentDescription = null, tint = contentColor)
                    Text(
                        text = routine.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                    StatusBadge(
                        text = when (routine.status) {
                            RoutineStatus.ACTIVE -> "Active"
                            RoutineStatus.DRAFT -> "Draft"
                            RoutineStatus.ARCHIVED -> "Archived"
                        },
                        color = when (routine.status) {
                            RoutineStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
                            RoutineStatus.DRAFT -> MaterialTheme.colorScheme.secondaryContainer
                            RoutineStatus.ARCHIVED -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                    IconButton(onClick = onClick) {
                        Icon(Icons.Filled.ModeEdit, contentDescription = "Edit ${routine.title}", tint = contentColor)
                    }
                }
                routine.description?.let {
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
                Text(
                    listOfNotNull(taskLabel, nextLabel).joinToString("  •  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor
                )
                if (routine.fileUris.isNotEmpty()) {
                    Text(
                        "${routine.fileUris.size} file(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }
    }
}
