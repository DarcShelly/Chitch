package com.chitch.app.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chitch.app.ui.components.AttachmentsSection
import com.chitch.app.ui.components.ExistingTaskPickerDialog
import sh.calvin.reorderable.ReorderableColumn

@Composable
fun EditRoutineScreen(
    viewModel: EditRoutineViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onEditTask: (Long) -> Unit,
    onAddNewTask: () -> Unit
) {
    val state = viewModel.uiState
    val tasks by viewModel.tasks.collectAsState()
    val availableTasks by viewModel.availableTasks.collectAsState()

    if (state.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.showTaskPicker) {
        ExistingTaskPickerDialog(
            tasks = availableTasks,
            onPick = { viewModel.addExistingTask(it.id) },
            onDismiss = viewModel::closeTaskPicker
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
    ) {
        Text("Edit Routine", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::updateTitle,
            label = { Text("Routine title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::updateDescription,
            label = { Text("Description") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        AttachmentsSection(
            imageUri = state.imageUri,
            fileUris = state.fileUris,
            onImagePicked = viewModel::updateImage,
            onFilesAdded = viewModel::addFiles,
            onRemoveFile = viewModel::removeFile
        )
        Spacer(Modifier.height(20.dp))

        Text("Tasks", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (tasks.isEmpty()) {
            Text("No tasks yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            ReorderableColumn(
                list = tasks,
                onSettle = { fromIndex, toIndex ->
                    val newOrder = tasks.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                    viewModel.reorderTasks(newOrder)
                }
            ) { _, task, _ ->
                key(task.id) {
                    ReorderableItem(modifier = Modifier.padding(vertical = 4.dp)) {
                        Card(
                            onClick = { onEditTask(task.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.DragHandle,
                                    contentDescription = "Drag to reorder ${task.title}",
                                    modifier = Modifier.draggableHandle()
                                )
                                Text(
                                    task.title,
                                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                                )
                                IconButton(onClick = { viewModel.removeTask(task.id) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Remove ${task.title}")
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = viewModel::openTaskPicker) {
                Text("+ Add existing task")
            }
            TextButton(onClick = onAddNewTask) {
                Text("+ Add new task")
            }
        }

        state.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = { viewModel.submit(onDone) },
                enabled = !state.saving,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (state.saving) "Saving…" else "Save changes")
            }
        }
    }
}
