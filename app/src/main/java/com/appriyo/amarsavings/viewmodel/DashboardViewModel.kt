package com.appriyo.amarsavings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.amarsavings.data.db.*
import com.appriyo.amarsavings.data.repository.SavingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val totalSaved: Long = 0L,
    val goal: Long = 0L,
    val recentTransactions: List<Transaction> = emptyList(),
    val noteDistribution: NoteDistribution = NoteDistribution(),
    val isLoading: Boolean = true
) {
    val remaining: Long get() = maxOf(0L, goal - totalSaved)
    val progressFraction: Float get() = if (goal > 0) (totalSaved.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val progressPercent: Int get() = (progressFraction * 100).toInt()
}

class DashboardViewModel(private val repo: SavingsRepository) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repo.totalSaved,
        repo.savingsGoal,
        repo.recentTransactions,
        repo.noteDistribution
    ) { saved, goal, recent, notes ->
        DashboardUiState(
            totalSaved = saved,
            goal = goal,
            recentTransactions = recent,
            noteDistribution = notes,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun setGoal(amount: Long) = viewModelScope.launch {
        repo.setSavingsGoal(amount)
    }

    fun deleteTransaction(transaction: Transaction) = viewModelScope.launch {
        repo.deleteTransaction(transaction)
    }
}