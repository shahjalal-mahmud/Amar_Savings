package com.appriyo.amarsavings.data.repository

import com.appriyo.amarsavings.data.db.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers

class SavingsRepository(
    private val dao: TransactionDao,
    private val prefs: AppPreferences
) {
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactions()
    val recentTransactions: Flow<List<Transaction>> = dao.getRecentTransactions()
    val totalSaved: Flow<Long> = dao.getTotalSaved()
    val noteDistribution: Flow<NoteDistribution> = dao.getNoteDistribution()
    val savingsGoal: Flow<Long> = prefs.savingsGoal

    /**
     * Emits a Unit every time local data is mutated. Used by [BackupScheduler]
     * to debounce and trigger an upload. Uses [MutableSharedFlow] (replay = 0,
     * extraBufferCapacity = 16) so we don't suspend writers or lose rapid edits.
     */
    private val _dirty = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 16)
    val dirty: SharedFlow<Unit> = _dirty.asSharedFlow()

    private suspend fun markDirty() { _dirty.tryEmit(Unit) }

    suspend fun addTransaction(transaction: Transaction): Long {
        val id = dao.insert(transaction)
        markDirty()
        return id
    }

    suspend fun updateTransaction(transaction: Transaction) {
        dao.update(transaction)
        markDirty()
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        dao.delete(transaction)
        markDirty()
    }

    suspend fun getTransactionById(id: Long): Transaction? = dao.getById(id)

    suspend fun setSavingsGoal(amount: Long) {
        prefs.setSavingsGoal(amount)
        markDirty()
    }

    // ── Backup helpers ─────────────────────────────────────────────────────

    /** One-shot snapshot used to build the backup payload. */
    suspend fun getAllTransactionsSnapshot(): List<Transaction> = dao.getAllOnce()

    suspend fun getSavingsGoalSnapshot(): Long = prefs.savingsGoal.first()

    /** Atomic restore: clear local DB and insert the supplied transactions. */
    suspend fun replaceAllTransactions(transactions: List<Transaction>) {
        dao.replaceAll(transactions)
        markDirty()
    }

    /**
     * Stable hash representing the current local data state. Used to detect
     * "dirty" changes since the last successful backup. SHA-256 is overkill but
     * cheap and gives us a stable, fixed-length identifier.
     */
    suspend fun computeLocalStateHash(): String {
        val transactions = dao.getAllOnce()
        val goal = prefs.savingsGoal.first()
        val sb = StringBuilder()
        sb.append("g:").append(goal).append('|')
        // Sort by id for deterministic ordering
        for (t in transactions.sortedBy { it.id }) {
            sb.append(t.id).append(':')
                .append(t.type.name).append(':')
                .append(t.timestamp).append(':')
                .append(t.qty1000).append(',')
                .append(t.qty500).append(',')
                .append(t.qty200).append(',')
                .append(t.qty100).append(',')
                .append(t.qty50).append(',')
                .append(t.qty20).append(',')
                .append(t.qty10).append(',')
                .append(t.qty5).append(',')
                .append(t.qty2).append(',')
                .append(t.qty1).append(',')
                .append(t.totalAmount).append(':')
                .append(t.note).append('|')
        }
        return sha256Hex(sb.toString())
    }

    private fun sha256Hex(input: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}