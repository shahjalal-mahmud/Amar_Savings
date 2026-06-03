package com.appriyo.amarsavings.data.repository

import com.appriyo.amarsavings.data.db.*
import kotlinx.coroutines.flow.Flow

class SavingsRepository(
    private val dao: TransactionDao,
    private val prefs: AppPreferences
) {
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactions()
    val recentTransactions: Flow<List<Transaction>> = dao.getRecentTransactions()
    val totalSaved: Flow<Long> = dao.getTotalSaved()
    val noteDistribution: Flow<NoteDistribution> = dao.getNoteDistribution()
    val savingsGoal: Flow<Long> = prefs.savingsGoal

    suspend fun addTransaction(transaction: Transaction): Long = dao.insert(transaction)

    suspend fun updateTransaction(transaction: Transaction) = dao.update(transaction)

    suspend fun deleteTransaction(transaction: Transaction) = dao.delete(transaction)

    suspend fun getTransactionById(id: Long): Transaction? = dao.getById(id)

    suspend fun setSavingsGoal(amount: Long) = prefs.setSavingsGoal(amount)
}