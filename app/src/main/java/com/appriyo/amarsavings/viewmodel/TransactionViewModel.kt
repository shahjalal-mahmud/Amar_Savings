package com.appriyo.amarsavings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.amarsavings.data.db.*
import com.appriyo.amarsavings.data.repository.SavingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TransactionUiState(
    val denominations: List<DenominationInput> = defaultDenominations(),
    val note: String = "",
    val type: TransactionType = TransactionType.ADD,
    val editingId: Long? = null,
    val isSaved: Boolean = false
) {
    val totalAmount: Long get() = denominations.sumOf { it.subtotal }
    val isValid: Boolean get() = totalAmount > 0
}

class TransactionViewModel(private val repo: SavingsRepository) : ViewModel() {

    private val _state = MutableStateFlow(TransactionUiState())
    val state: StateFlow<TransactionUiState> = _state.asStateFlow()

    fun initForAdd(type: TransactionType) {
        _state.value = TransactionUiState(type = type)
    }

    fun initForEdit(transaction: Transaction) {
        _state.value = TransactionUiState(
            denominations = transaction.toDenominationInputs(),
            note = transaction.note,
            type = transaction.type,
            editingId = transaction.id
        )
    }

    fun updateQuantity(denomination: Int, quantity: Int) {
        val updated = _state.value.denominations.map {
            if (it.denomination == denomination) it.copy(quantity = maxOf(0, quantity)) else it
        }
        _state.value = _state.value.copy(denominations = updated)
    }

    fun incrementQuantity(denomination: Int) {
        val current = _state.value.denominations.find { it.denomination == denomination }?.quantity ?: 0
        updateQuantity(denomination, current + 1)
    }

    fun decrementQuantity(denomination: Int) {
        val current = _state.value.denominations.find { it.denomination == denomination }?.quantity ?: 0
        updateQuantity(denomination, maxOf(0, current - 1))
    }

    fun updateNote(note: String) {
        _state.value = _state.value.copy(note = note)
    }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (!s.isValid) return@launch

        val tx = s.denominations.toTransaction(s.type, s.note)
        if (s.editingId != null) {
            repo.updateTransaction(tx.copy(id = s.editingId))
        } else {
            repo.addTransaction(tx)
        }
        _state.value = _state.value.copy(isSaved = true)
    }

    fun resetSaved() {
        _state.value = _state.value.copy(isSaved = false)
    }

    fun clearAll() {
        _state.value = TransactionUiState(type = _state.value.type)
    }
}