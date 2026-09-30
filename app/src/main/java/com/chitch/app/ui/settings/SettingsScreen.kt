package com.chitch.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chitch.app.data.entity.UserEntity
import com.chitch.app.ui.components.TimePickerField
import java.time.LocalTime

/**
 * Minimal settings screen — only the two fields routine-hour validation needs
 * (working hours, sleep hours). See clarification #7 in chat.
 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val user by viewModel.user.collectAsState()
    val backupState by viewModel.backupState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var workStart by remember { mutableStateOf<LocalTime?>(null) }
    var workEnd by remember { mutableStateOf<LocalTime?>(null) }
    var sleepHours by remember { mutableIntStateOf(UserEntity.DEFAULT_SLEEP_HOURS) }
    var maxMissedDays by remember { mutableIntStateOf(UserEntity.DEFAULT_MAX_MISSED_DAYS) }

    LaunchedEffect(user) {
        user?.let {
            workStart = it.workStart
            workEnd = it.workEnd
            sleepHours = it.sleepHours
            maxMissedDays = it.maxMissedDaysForHabitCompletion
        }
    }

    LaunchedEffect(backupState.message) {
        backupState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissBackupMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let { viewModel.exportBackup(context, it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBackup(context, it) } }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))

            Text("Working hours", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimePickerField(
                    label = "Start",
                    time = workStart,
                    onTimeSelected = { workStart = it },
                    modifier = Modifier.weight(1f)
                )
                TimePickerField(
                    label = "End",
                    time = workEnd,
                    onTimeSelected = { workEnd = it },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(24.dp))

            Text(
                "Sleep hours (minimum ${UserEntity.MIN_SLEEP_HOURS})",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    sleepHours = (sleepHours - 1).coerceAtLeast(UserEntity.MIN_SLEEP_HOURS)
                }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease sleep hours")
                }
                Text("$sleepHours h", modifier = Modifier.padding(horizontal = 16.dp))
                IconButton(onClick = { sleepHours += 1 }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase sleep hours")
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Habit misses allowed per month before it stops counting as built",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    maxMissedDays = (maxMissedDays - 1).coerceAtLeast(UserEntity.MIN_MAX_MISSED_DAYS)
                }) {
                    Icon(Icons.Filled.Remove, contentDescription = "Decrease allowed misses")
                }
                Text("$maxMissedDays day(s)", modifier = Modifier.padding(horizontal = 16.dp))
                IconButton(onClick = { maxMissedDays += 1 }) {
                    Icon(Icons.Filled.Add, contentDescription = "Increase allowed misses")
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { viewModel.save(workStart, workEnd, sleepHours, maxMissedDays) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }

            Spacer(Modifier.height(32.dp))
            Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Export a copy of everything (tasks, habits, routines, stats) to a file you choose, " +
                    "or restore from one. Restoring replaces everything currently in the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("chitch-backup.db") },
                    enabled = !backupState.busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Export backup")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    enabled = !backupState.busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restore backup")
                }
            }
        }
    }
}
