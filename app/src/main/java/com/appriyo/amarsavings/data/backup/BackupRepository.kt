package com.appriyo.amarsavings.data.backup

import com.appriyo.amarsavings.data.auth.AuthRepository
import com.appriyo.amarsavings.data.auth.AuthState
import com.appriyo.amarsavings.data.db.AppPreferences
import com.appriyo.amarsavings.data.db.Transaction
import com.appriyo.amarsavings.data.repository.SavingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class BackupRepository(
    private val auth: AuthRepository,
    private val prefs: AppPreferences,
    private val drive: DriveBackupClient,
    private val savings: SavingsRepository,
    private val json: Json
) {
    private val mutex = Mutex()

    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state.asStateFlow()

    /** Builds a snapshot [BackupFile] of the current local state. */
    suspend fun buildSnapshot(): BackupFile {
        val email = when (val s = auth.state.value) {
            is AuthState.SignedIn -> s.email
            is AuthState.Restoring -> s.email
            else -> null
        }
        return BackupFile(
            version = 1,
            createdAt = System.currentTimeMillis(),
            userEmail = email,
            savingsGoal = savings.getSavingsGoalSnapshot(),
            transactions = savings.getAllTransactionsSnapshot().map { it.toDto() }
        )
    }

    /** Uploads the current local state to Drive. */
    suspend fun uploadNow(): Result<Unit> {
        if (auth.state.value !is AuthState.SignedIn) {
            return Result.failure(IllegalStateException("Not signed in"))
        }
        if (!auth.ensureDriveAccess()) {
            return Result.failure(IllegalStateException("Drive authorization required — check driveConsentIntent"))
        }
        return mutex.withLock {
            _state.value = BackupState.Syncing
            val snapshot = buildSnapshot()
            val bytes = json.encodeToString(BackupFile.serializer(), snapshot)
                .toByteArray(Charsets.UTF_8)
            val uploaded = drive.upload(bytes)
            uploaded.fold(
                onSuccess = { fileId ->
                    prefs.setDriveBackupFileId(fileId)
                    prefs.setLastBackupAt(snapshot.createdAt)
                    prefs.setLocalStateHash(savings.computeLocalStateHash())
                    _state.value = BackupState.SyncedAt(snapshot.createdAt)
                    Result.success(Unit)
                },
                onFailure = { t ->
                    _state.value = BackupState.Failed(t.message ?: "Upload failed")
                    Result.failure(t)
                }
            )
        }
    }

    /**
     * Downloads and applies the user's backup to local storage.
     * Returns a [RestoreOutcome] describing what happened.
     */
    suspend fun restoreIfAny(): RestoreOutcome {
        // Called by BackupScheduler while auth state is Restoring, not yet SignedIn.
        if (auth.state.value !is AuthState.Restoring) return RestoreOutcome.Idle
        if (!auth.ensureDriveAccess()) return RestoreOutcome.Failed("Drive authorization required")

        return mutex.withLock {
            val downloaded = drive.download()
            downloaded.fold(
                onSuccess = { backup ->
                    if (backup == null) {
                        prefs.setLastBackupAt(0L)
                        prefs.setLocalStateHash(savings.computeLocalStateHash())
                        RestoreOutcome.NoBackup
                    } else {
                        applyRestore(backup)
                    }
                },
                onFailure = { t ->
                    RestoreOutcome.Failed(t.message ?: "Download failed")
                }
            )
        }
    }

    private suspend fun applyRestore(backup: BackupFile): RestoreOutcome {
        return try {
            val transactions = backup.transactions.map { it.toEntity() }
            savings.replaceAllTransactions(transactions)
            prefs.setSavingsGoal(backup.savingsGoal)
            prefs.setLastBackupAt(backup.createdAt)
            prefs.setLocalStateHash(savings.computeLocalStateHash())
            drive.getMeta().getOrNull()?.fileId?.let { prefs.setDriveBackupFileId(it) }
            _state.value = BackupState.SyncedAt(backup.createdAt)
            RestoreOutcome.Restored(transactions.size)
        } catch (t: Throwable) {
            RestoreOutcome.Failed(t.message ?: "Restore failed")
        }
    }
}

private fun Transaction.toDto() = TransactionDto(
    id = id,
    type = type.name,
    timestamp = timestamp,
    qty1000 = qty1000, qty500 = qty500, qty200 = qty200,
    qty100 = qty100, qty50 = qty50, qty20 = qty20,
    qty10 = qty10, qty5 = qty5, qty2 = qty2, qty1 = qty1,
    totalAmount = totalAmount,
    note = note
)

private fun TransactionDto.toEntity(): Transaction {
    val type = runCatching { com.appriyo.amarsavings.data.db.TransactionType.valueOf(type) }
        .getOrDefault(com.appriyo.amarsavings.data.db.TransactionType.ADD)
    return Transaction(
        id = id,
        type = type,
        timestamp = timestamp,
        qty1000 = qty1000, qty500 = qty500, qty200 = qty200,
        qty100 = qty100, qty50 = qty50, qty20 = qty20,
        qty10 = qty10, qty5 = qty5, qty2 = qty2, qty1 = qty1,
        totalAmount = totalAmount,
        note = note
    )
}