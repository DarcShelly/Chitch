package com.chitch.app.ui.habit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chitch.app.data.entity.RepeatType
import com.chitch.app.ui.components.AttachmentsSection
import com.chitch.app.ui.components.DatePickerField
import com.chitch.app.ui.components.RepeatTypeDropdown
import com.chitch.app.ui.components.TimePickerField
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun CreateHabitScreen(
    viewModel: CreateHabitViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val state = viewModel.uiState

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
    ) {
        Text("Create Habit", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Text("Start from a template (optional)", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HABIT_TEMPLATES.forEach { template ->
                AssistChip(
                    onClick = { viewModel.applyTemplate(template) },
                    label = { Text(template.title) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::updateTitle,
            label = { Text("Habit title") },
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
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Activate immediately", modifier = Modifier.weight(1f))
            Switch(checked = state.activateNow, onCheckedChange = viewModel::updateActivateNow)
        }
        Spacer(Modifier.height(20.dp))

        Text("Tasks", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        state.tasks.forEach { row ->
            HabitTaskRowEditor(
                row = row,
                onTitleChange = { viewModel.updateRowTitle(row.rowId, it) },
                onHasTimeChange = { viewModel.updateRowHasTime(row.rowId, it) },
                onStartTimeChange = { viewModel.updateRowStartTime(row.rowId, it) },
                onDueTimeChange = { viewModel.updateRowDueTime(row.rowId, it) },
                onRepeatTypeChange = { viewModel.updateRowRepeatType(row.rowId, it) },
                onScheduledDateChange = { viewModel.updateRowScheduledDate(row.rowId, it) },
                onRemove = if (state.tasks.size > 1) {
                    { viewModel.removeTaskRow(row.rowId) }
                } else null
            )
            Spacer(Modifier.height(12.dp))
        }

        TextButton(onClick = viewModel::addTaskRow) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("Add task", modifier = Modifier.padding(start = 4.dp))
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
                Text(if (state.saving) "Saving…" else "Save habit")
            }
        }
    }
}

@Composable
private fun HabitTaskRowEditor(
    row: HabitTaskRow,
    onTitleChange: (String) -> Unit,
    onHasTimeChange: (Boolean) -> Unit,
    onStartTimeChange: (LocalTime?) -> Unit,
    onDueTimeChange: (LocalTime?) -> Unit,
    onRepeatTypeChange: (RepeatType) -> Unit,
    onScheduledDateChange: (LocalDate?) -> Unit,
    onRemove: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = row.title,
                    onValueChange = onTitleChange,
                    label = { Text("Task title") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (onRemove != null) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove task")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Has a specific time", modifier = Modifier.weight(1f))
                Switch(checked = row.hasTime, onCheckedChange = onHasTimeChange)
            }
            if (row.hasTime) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimePickerField(
                        label = "Start",
                        time = row.startTime,
                        onTimeSelected = onStartTimeChange,
                        allowClear = false,
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerField(
                        label = "End",
                        time = row.dueTime,
                        onTimeSelected = onDueTimeChange,
                        allowClear = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            RepeatTypeDropdown(
                value = row.repeatType,
                onValueChange = onRepeatTypeChange,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            DatePickerField(
                label = "Scheduled date (optional)",
                date = row.scheduledDate,
                onDateSelected = onScheduledDateChange,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
