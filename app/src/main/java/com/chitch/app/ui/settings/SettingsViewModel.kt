package com.chitch.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chitch.app.data.BackupManager
import com.chitch.app.data.entity.UserEntity
import com.chitch.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

data class BackupUiState(val busy: Boolean = false, val message: String? = null)

class SettingsViewModel(private val repository: UserRepository) : ViewModel() {

    val user: StateFlow<UserEntity?> = repository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(workStart: LocalTime?, workEnd: LocalTime?, sleepHours: Int, maxMissedDaysForHabitCompletion: Int) {
        viewModelScope.launch { repository.save(workStart, workEnd, sleepHours, maxMissedDaysForHabitCompletion) }
    }

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    fun exportBackup(context: Context, targetUri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupUiState(busy = true)
            _backupState.value = try {
                BackupManager.exportBackup(context, targetUri)
                BackupUiState(message = "Backup saved.")
            } catch (e: Exception) {
                BackupUiState(message = "Couldn't save the backup: ${e.message}")
            }
        }
    }

    /** On success this restarts the app (see [BackupManager.importBackup]) — [context] must be the Activity. */
    fun importBackup(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupUiState(busy = true)
            try {
                BackupManager.importBackup(context, sourceUri)
            } catch (e: Exception) {
                _backupState.value = BackupUiState(message = "Couldn't restore the backup: ${e.message}")
            }
        }
    }

    fun dismissBackupMessage() {
        _backupState.value = _backupState.value.copy(message = null)
    }
}
