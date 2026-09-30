package com.chitch.app.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chitch.app.data.repository.SummaryRange
import com.chitch.app.domain.PeriodSummary
import kotlin.math.roundToInt

@Composable
fun SummaryScreen(viewModel: SummaryViewModel) {
    val range by viewModel.range.collectAsState()
    val summary by viewModel.summary.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Summary", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                SummaryRange.YESTERDAY to "Yesterday",
                SummaryRange.WEEK to "This week",
                SummaryRange.MONTH to "This month"
            ).forEach { (r, label) ->
                if (r == range) {
                    Button(onClick = { viewModel.selectRange(r) }) { Text(label) }
                } else {
                    OutlinedButton(onClick = { viewModel.selectRange(r) }) { Text(label) }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        if (summary.dayCount == 0) {
            Text(
                "No reviews logged for this period yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            SummaryStatCard("Days reviewed", summary.dayCount.toString())
            summary.averageMood?.let {
                SummaryStatCard("Average mood", moodLabel(it))
            }
            SummaryStatCard("Tasks done", summary.totalTasksDone.toString())
            SummaryStatCard("Tasks missed", summary.totalTasksMissed.toString())
            SummaryStatCard("Habits checked", summary.totalHabitsChecked.toString())
            SummaryStatCard("Habits missed", summary.totalHabitsMissed.toString())
            summary.bestDay?.let { SummaryStatCard("Best day", it.toString()) }
            summary.toughestDay?.let { SummaryStatCard("Toughest day", it.toString()) }
        }
    }
}

private fun moodLabel(average: Double): String {
    val ordinal = average.roundToInt().coerceIn(0, 4)
    val name = com.chitch.app.data.entity.MoodRating.entries[ordinal].name
        .lowercase()
        .replace('_', ' ')
    return "%.1f / 4 (%s)".format(average, name)
}

@Composable
private fun SummaryStatCard(label: String, value: String) {
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
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
