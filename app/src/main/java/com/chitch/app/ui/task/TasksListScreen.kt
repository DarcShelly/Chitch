package com.chitch.app.ui.task

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.data.entity.TaskStatus
import com.chitch.app.domain.isCompletedNow
import com.chitch.app.domain.sortedForDisplayBy
import com.chitch.app.ui.components.FullWidthCta
import com.chitch.app.ui.components.ImageBackdrop
import com.chitch.app.ui.components.StatusBadge
import com.chitch.app.ui.components.repeatTypeLabel

@Composable
fun TasksListScreen(
    viewModel: TasksListViewModel,
    searchQuery: String,
    onCreateTask: () -> Unit,
    onEditTask: (Long) -> Unit
) {
    val allTasks by viewModel.tasks.collectAsState()
    val undoEvent by viewModel.undoEvent.collectAsState()
    val query = searchQuery.trim()
    val filtered = if (query.isBlank()) {
        allTasks
    } else {
        allTasks.filter { it.task.title.contains(query, ignoreCase = true) }
    }
    val sorted = filtered.sortedForDisplayBy { it.task }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(undoEvent) {
        val event = undoEvent ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(event.actionLabel, actionLabel = "Undo")
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undo(event)
        }
        viewModel.dismissUndoEvent()
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Tasks", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            when {
                allTasks.isEmpty() -> Column {
                    Text(
                        "Nothing on your list yet — add the first thing you need to get done.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    FullWidthCta(label = "Add a Task", onClick = onCreateTask, icon = Icons.Outlined.RadioButtonUnchecked)
                }
                sorted.isEmpty() -> Text(
                    "No tasks match \"$searchQuery\".",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(sorted, key = { it.task.id }) { item ->
                        val task = item.task
                        SwipeableTaskCard(
                            task = task,
                            parentLabel = item.parentLabel,
                            onComplete = { viewModel.completeTask(task) },
                            onUncomplete = { viewModel.uncompleteTask(task.id) },
                            onArchive = { viewModel.archiveTask(task) },
                            onEdit = { onEditTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableTaskCard(
    task: TaskEntity,
    parentLabel: String?,
    onComplete: () -> Unit,
    onUncomplete: () -> Unit,
    onArchive: () -> Unit,
    onEdit: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (task.isCompletedNow()) onUncomplete() else onComplete()
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onArchive()
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeActionBackground(dismissState.dismissDirection, task.isCompletedNow()) }
    ) {
        TaskDetailCard(task, parentLabel = parentLabel, onEdit = onEdit)
    }
}

@Composable
private fun SwipeActionBackground(direction: SwipeToDismissBoxValue, completedNow: Boolean) {
    val (color, icon, alignment) = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            if (completedNow) Icons.Filled.ExpandLess else Icons.Filled.CheckCircle,
            Alignment.CenterStart
        )
        SwipeToDismissBoxValue.EndToStart -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            Icons.Filled.Archive,
            Alignment.CenterEnd
        )
        SwipeToDismissBoxValue.Settled -> Triple(Color.Transparent, null, Alignment.Center)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color, RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        icon?.let { Icon(it, contentDescription = null) }
    }
}

@Composable
private fun TaskDetailCard(task: TaskEntity, parentLabel: String?, onEdit: () -> Unit) {
    var expanded by remember(task.id) { mutableStateOf(false) }
    val hasImage = task.imageUri != null
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
            .then(if (hasImage && !expanded) Modifier.height(180.dp) else Modifier)
            .clickable { expanded = !expanded }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            task.imageUri?.let { ImageBackdrop(it) }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (task.isCompletedNow()) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = contentColor
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )
                    if (task.status == TaskStatus.ARCHIVED) {
                        StatusBadge("Archived", MaterialTheme.colorScheme.errorContainer)
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = mutedColor
                    )
                }
                task.description?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = mutedColor,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                val details = buildList {
                    if (task.startTime != null && task.dueTime != null) add("${task.startTime}–${task.dueTime}")
                    add(repeatTypeLabel(task.repeatType))
                    task.scheduledDate?.let { add("From $it") }
                    if (task.notify) add("Notify on")
                    if (task.fileUris.isNotEmpty()) add("${task.fileUris.size} file(s)")
                    parentLabel?.let { add("Part of: $it") }
                }
                Text(
                    details.joinToString("  •  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor
                )

                AnimatedVisibility(visible = expanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.ModeEdit, contentDescription = null, modifier = Modifier.height(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit")
                        }
                    }
                }
            }
        }
    }
}
