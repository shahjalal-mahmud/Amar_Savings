package com.appriyo.amarsavings.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType { ADD, WITHDRAW }

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val timestamp: Long = System.currentTimeMillis(),

    // Denomination quantities
    val qty1000: Int = 0,
    val qty500: Int = 0,
    val qty200: Int = 0,
    val qty100: Int = 0,
    val qty50: Int = 0,
    val qty20: Int = 0,
    val qty10: Int = 0,
    val qty5: Int = 0,
    val qty2: Int = 0,
    val qty1: Int = 0,

    val totalAmount: Long = 0L,
    val note: String = ""
) {
    fun denominationBreakdown(): List<Pair<Int, Int>> = listOf(
        1000 to qty1000, 500 to qty500, 200 to qty200,
        100 to qty100, 50 to qty50, 20 to qty20,
        10 to qty10, 5 to qty5, 2 to qty2, 1 to qty1
    ).filter { it.second > 0 }

    fun totalNotes(): Int = qty1000 + qty500 + qty200 + qty100 + qty50 + qty20 + qty10 + qty5 + qty2 + qty1
}

data class DenominationInput(
    val denomination: Int,
    var quantity: Int = 0
) {
    val subtotal: Long get() = denomination.toLong() * quantity
}

fun defaultDenominations() = listOf(
    DenominationInput(1000), DenominationInput(500), DenominationInput(200),
    DenominationInput(100), DenominationInput(50), DenominationInput(20),
    DenominationInput(10), DenominationInput(5), DenominationInput(2), DenominationInput(1)
)

fun List<DenominationInput>.toTransaction(type: TransactionType, note: String = ""): Transaction {
    val map = associateBy { it.denomination }
    return Transaction(
        type = type,
        qty1000 = map[1000]?.quantity ?: 0,
        qty500  = map[500]?.quantity  ?: 0,
        qty200  = map[200]?.quantity  ?: 0,
        qty100  = map[100]?.quantity  ?: 0,
        qty50   = map[50]?.quantity   ?: 0,
        qty20   = map[20]?.quantity   ?: 0,
        qty10   = map[10]?.quantity   ?: 0,
        qty5    = map[5]?.quantity    ?: 0,
        qty2    = map[2]?.quantity    ?: 0,
        qty1    = map[1]?.quantity    ?: 0,
        totalAmount = sumOf { it.subtotal },
        note = note
    )
}

fun Transaction.toDenominationInputs(): List<DenominationInput> = listOf(
    DenominationInput(1000, qty1000), DenominationInput(500, qty500),
    DenominationInput(200, qty200), DenominationInput(100, qty100),
    DenominationInput(50, qty50), DenominationInput(20, qty20),
    DenominationInput(10, qty10), DenominationInput(5, qty5),
    DenominationInput(2, qty2), DenominationInput(1, qty1)
)