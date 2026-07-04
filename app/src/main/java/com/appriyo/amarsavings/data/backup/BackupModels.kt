package com.appriyo.amarsavings.data.backup

import kotlinx.serialization.Serializable

/**
 * Versioned representation of a single transaction in the backup file.
 * Using a DTO (rather than serializing the Room entity directly) means we can
 * evolve the on-device schema without breaking the cloud format.
 */
@Serializable
data class TransactionDto(
    val id: Long,
    val type: String,           // "ADD" or "WITHDRAW"
    val timestamp: Long,
    val qty1000: Int,
    val qty500: Int,
    val qty200: Int,
    val qty100: Int,
    val qty50: Int,
    val qty20: Int,
    val qty10: Int,
    val qty5: Int,
    val qty2: Int,
    val qty1: Int,
    val totalAmount: Long,
    val note: String
)

@Serializable
data class BackupFile(
    /** Bump on breaking changes so the restore can refuse / migrate. */
    val version: Int = 1,
    val createdAt: Long,
    val userEmail: String? = null,
    val savingsGoal: Long = 0,
    val transactions: List<TransactionDto> = emptyList()
)

/** Lightweight metadata returned by the Drive API. */
data class BackupMeta(
    val fileId: String,
    val modifiedTimeMs: Long?
)

/** Outcome of a restore attempt. */
sealed class RestoreOutcome {
    object Idle : RestoreOutcome()
    object NoBackup : RestoreOutcome()
    data class Restored(val transactionCount: Int) : RestoreOutcome()
    data class Failed(val reason: String) : RestoreOutcome()
}

/** Run-time state of the backup subsystem, observed by the UI. */
sealed class BackupState {
    object Idle : BackupState()
    object Syncing : BackupState()
    data class SyncedAt(val timestampMs: Long) : BackupState()
    data class Failed(val reason: String) : BackupState()
    object Offline : BackupState()
}