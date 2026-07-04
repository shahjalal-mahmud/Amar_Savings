package com.appriyo.amarsavings.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.amarsavings.data.auth.AuthRepository
import com.appriyo.amarsavings.data.auth.AuthState
import com.appriyo.amarsavings.data.backup.BackupRepository
import com.appriyo.amarsavings.data.backup.BackupScheduler
import com.appriyo.amarsavings.data.backup.BackupState
import com.appriyo.amarsavings.data.db.AppPreferences
import com.appriyo.amarsavings.data.repository.SavingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val authState: AuthState = AuthState.SignedOut,
    val backupState: BackupState = BackupState.Idle,
    val lastBackupAt: Long = 0L,
    val localTransactionCount: Int = 0
)

class SettingsViewModel(
    private val auth: AuthRepository,
    private val backup: BackupRepository,
    private val scheduler: BackupScheduler,
    private val prefs: AppPreferences,
    private val savings: SavingsRepository
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        auth.state,
        backup.state,
        prefs.lastBackupAt,
        savings.allTransactions
    ) { a, b, last, tx ->
        SettingsUiState(
            authState = a,
            backupState = b,
            lastBackupAt = last,
            localTransactionCount = tx.size
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    fun snackShown() { _snackbar.value = null }

    fun backupNow() {
        viewModelScope.launch {
            val result = backup.uploadNow()
            _snackbar.value = result.fold(
                onSuccess = { "Backup complete" },
                onFailure = { "Backup failed: ${it.message ?: "unknown error"}" }
            )
        }
    }

    fun restore() {
        viewModelScope.launch {
            val outcome = backup.restoreIfAny()
            _snackbar.value = when (outcome) {
                is com.appriyo.amarsavings.data.backup.RestoreOutcome.NoBackup -> "No backup found in this account."
                is com.appriyo.amarsavings.data.backup.RestoreOutcome.Restored -> "Restored ${outcome.transactionCount} transactions."
                is com.appriyo.amarsavings.data.backup.RestoreOutcome.Failed -> "Restore failed: ${outcome.reason}"
                else -> null
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            _snackbar.value = "Signed out. Your local data is still here."
        }
    }
}