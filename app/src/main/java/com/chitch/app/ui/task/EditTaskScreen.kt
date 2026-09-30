package com.chitch.app.ui.task

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chitch.app.data.entity.HabitEntity
import com.chitch.app.data.entity.RepeatType
import com.chitch.app.data.entity.RoutineEntity
import com.chitch.app.ui.components.AttachmentsSection
import com.chitch.app.ui.components.DatePickerField
import com.chitch.app.ui.components.DayOfWeekSelector
import com.chitch.app.ui.components.RepeatTypeDropdown
import com.chitch.app.ui.components.TimePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskScreen(
    viewModel: EditTaskViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val state = viewModel.uiState
    val routines by viewModel.routines.collectAsState()
    val habits by viewModel.habits.collectAsState()

    if (state.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (state.notFound) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Task not found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
    ) {
        Text("Edit Task", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::updateTitle,
            label = { Text("Title") },
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

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimePickerField(
                label = "Start time",
                time = state.startTime,
                onTimeSelected = viewModel::updateStartTime,
                modifier = Modifier.weight(1f)
            )
            TimePickerField(
                label = "Due time",
                time = state.dueTime,
                onTimeSelected = viewModel::updateDueTime,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))

        RepeatTypeDropdown(
            value = state.repeatType,
            onValueChange = viewModel::updateRepeatType,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        if (state.repeatType == RepeatType.WEEKLY) {
            DayOfWeekSelector(
                selectedDays = state.selectedDaysOfWeek,
                onToggleDay = viewModel::toggleDayOfWeek
            )
            Spacer(Modifier.height(12.dp))
        }

        DatePickerField(
            label = "Scheduled date (optional)",
            date = state.scheduledDate,
            onDateSelected = viewModel::updateScheduledDate,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Notify", modifier = Modifier.weight(1f))
            Switch(checked = state.notify, onCheckedChange = viewModel::updateNotify)
        }
        Spacer(Modifier.height(16.dp))

        Text("Attach to", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        RoutinePicker(
            routines = routines,
            selectedId = state.routineId,
            onSelect = viewModel::updateRoutineId,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        HabitPicker(
            habits = habits,
            selectedId = state.habitId,
            onSelect = viewModel::updateHabitId,
            modifier = Modifier.fillMaxWidth()
        )

        state.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { viewModel.complete(onDone) }, modifier = Modifier.weight(1f)) {
                Text("Complete")
            }
            OutlinedButton(onClick = { viewModel.archive(onDone) }, modifier = Modifier.weight(1f)) {
                Text("Archive")
            }
        }
        Spacer(Modifier.height(12.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutinePicker(
    routines: List<RoutineEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = routines.firstOrNull { it.id == selectedId }?.title ?: "None"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            readOnly = true,
            value = label,
            onValueChange = {},
            label = { Text("Routine") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(null); expanded = false })
            routines.forEach { routine ->
                DropdownMenuItem(
                    text = { Text(routine.title) },
                    onClick = { onSelect(routine.id); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitPicker(
    habits: List<HabitEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = habits.firstOrNull { it.id == selectedId }?.title ?: "None"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            readOnly = true,
            value = label,
            onValueChange = {},
            label = { Text("Habit") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(null); expanded = false })
            habits.forEach { habit ->
                DropdownMenuItem(
                    text = { Text(habit.title) },
                    onClick = { onSelect(habit.id); expanded = false }
                )
            }
        }
    }
}
