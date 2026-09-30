package com.chitch.app.ui.dailyreview

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
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chitch.app.data.entity.HabitEntity
import com.chitch.app.data.entity.MoodRating
import com.chitch.app.data.entity.TaskEntity
import com.chitch.app.domain.isCompletedNow

@Composable
fun DailyReviewScreen(viewModel: DailyReviewViewModel) {
    val state = viewModel.uiState
    val activeTasks by viewModel.activeTasks.collectAsState()
    val activeHabits by viewModel.activeHabits.collectAsState()

    if (state.loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.celebrateHabits.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCelebration,
            confirmButton = {
                Button(onClick = viewModel::dismissCelebration) { Text("Nice!") }
            },
            title = { Text("🎉 Habit built!") },
            text = {
                Text(
                    "You've kept up \"${state.celebrateHabits.joinToString(", ") { it.title }}\" " +
                        "consistently for a month — it's marked completed."
                )
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
    ) {
        Text("Daily Review", style = MaterialTheme.typography.titleLarge)
        Text(
            viewModel.today.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        Text("How was today?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        MoodPicker(selected = state.mood, onSelect = viewModel::updateMood)
        Spacer(Modifier.height(24.dp))

        Text("Today's tasks", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        if (activeTasks.isEmpty()) {
            Text("No active tasks.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            activeTasks.forEach { task ->
                TaskCheckRow(task, onToggle = { viewModel.toggleTask(task.id, task.isCompletedNow()) })
            }
        }
        Spacer(Modifier.height(24.dp))

        Text("Habit checklist", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        if (activeHabits.isEmpty()) {
            Text("No active habits.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            activeHabits.forEach { habit ->
                HabitCheckRow(
                    habit,
                    checked = habit.id in state.checkedHabitIds,
                    onToggle = { viewModel.toggleHabitChecked(habit.id) }
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = state.thingsLiked,
            onValueChange = viewModel::updateThingsLiked,
            label = { Text("Things I liked") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.thingsMissed,
            onValueChange = viewModel::updateThingsMissed,
            label = { Text("Things I missed / didn't like") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.notesForTomorrow,
            onValueChange = viewModel::updateNotesForTomorrow,
            label = { Text("Notes for tomorrow") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        Button(
            onClick = viewModel::submit,
            enabled = !state.submitting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    state.submitting -> "Saving…"
                    state.alreadySubmittedToday -> "Update review"
                    else -> "Submit review"
                }
            )
        }

        if (state.suggestions.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("Suggestions for tomorrow", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.suggestions.forEach { suggestion ->
                        Text("• $suggestion", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private val MOOD_ORDER = listOf(
    MoodRating.VERY_SAD to Icons.Filled.SentimentVeryDissatisfied,
    MoodRating.SAD to Icons.Filled.SentimentDissatisfied,
    MoodRating.NEUTRAL to Icons.Filled.SentimentNeutral,
    MoodRating.HAPPY to Icons.Filled.SentimentSatisfied,
    MoodRating.VERY_HAPPY to Icons.Filled.SentimentVerySatisfied
)

@Composable
private fun MoodPicker(selected: MoodRating, onSelect: (MoodRating) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MOOD_ORDER.forEach { (mood, icon) ->
            val isSelected = mood == selected
            IconButton(onClick = { onSelect(mood) }) {
                Icon(
                    imageVector = icon,
                    contentDescription = mood.name,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.height(if (isSelected) 36.dp else 28.dp)
                )
            }
        }
    }
}

@Composable
private fun TaskCheckRow(task: TaskEntity, onToggle: () -> Unit) {
    val done = task.isCompletedNow()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = done, onCheckedChange = { onToggle() })
        Text(task.title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun HabitCheckRow(habit: HabitEntity, checked: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(habit.title, style = MaterialTheme.typography.bodyLarge)
    }
}
