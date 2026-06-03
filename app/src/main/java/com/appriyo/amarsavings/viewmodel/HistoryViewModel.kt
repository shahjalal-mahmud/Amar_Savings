package com.appriyo.amarsavings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.amarsavings.data.db.Transaction
import com.appriyo.amarsavings.data.repository.SavingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true
)

class HistoryViewModel(private val repo: SavingsRepository) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = repo.allTransactions
        .map { HistoryUiState(transactions = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repo.deleteTransaction(transaction)
    }
}