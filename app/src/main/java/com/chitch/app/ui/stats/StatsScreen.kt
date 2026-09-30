package com.chitch.app.ui.stats

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chitch.app.data.entity.TaskLedgerEntity

private data class StatRow(val label: String, val value: Int)

@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val ledger by viewModel.ledger.collectAsState()
    val habitStreaks by viewModel.habitStreaks.collectAsState()
    val rows = ledgerRows(ledger)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Stats", style = MaterialTheme.typography.titleLarge)
        Text(
            "Lifetime counts across all your tasks, habits, and routines.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn {
            if (habitStreaks.isNotEmpty()) {
                item {
                    Text(
                        "Habit streaks",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                items(habitStreaks, key = { "streak-${it.habit.id}" }) { streakItem ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(streakItem.habit.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(
                                "${streakItem.currentStreak}-day streak",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                item {
                    Text(
                        "Lifetime counts",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
            }
            items(rows, key = { it.label }) { row ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            row.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            row.value.toString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

private fun ledgerRows(ledger: TaskLedgerEntity): List<StatRow> = listOf(
    StatRow("Standalone tasks completed", ledger.standaloneTasksCompleted),
    StatRow("Routine tasks completed", ledger.routineTasksCompleted),
    StatRow("Habit tasks completed", ledger.habitTasksCompleted),
    StatRow("Habits completed", ledger.habitsCompleted),
    StatRow("Habits abandoned", ledger.habitsAbandoned),
    StatRow("Tasks missed", ledger.tasksMissed),
    StatRow("Times a routine was edited", ledger.routineEditCount)
)
