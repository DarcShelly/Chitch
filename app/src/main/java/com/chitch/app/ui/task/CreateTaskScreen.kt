package com.chitch.app.ui.task

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chitch.app.data.entity.RepeatType
import com.chitch.app.ui.components.AttachmentsSection
import com.chitch.app.ui.components.DatePickerField
import com.chitch.app.ui.components.DayOfWeekSelector
import com.chitch.app.ui.components.RepeatTypeDropdown
import com.chitch.app.ui.components.TimePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    viewModel: CreateTaskViewModel,
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
        Text("Create Task", style = MaterialTheme.typography.titleLarge)
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
                Text(if (state.saving) "Saving…" else "Create")
            }
        }
    }
}
