package com.appriyo.amarsavings.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 5")
    fun getRecentTransactions(): Flow<List<Transaction>>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'ADD' THEN totalAmount ELSE -totalAmount END), 0)
        FROM transactions
    """)
    fun getTotalSaved(): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(CASE WHEN type = 'ADD' THEN qty1000 ELSE -qty1000 END), 0) AS n1000,
               COALESCE(SUM(CASE WHEN type = 'ADD' THEN qty500  ELSE -qty500  END), 0) AS n500,
               COALESCE(SUM(CASE WHEN type = 'ADD' THEN qty200  ELSE -qty200  END), 0) AS n200,
               COALESCE(SUM(CASE WHEN type = 'ADD' THEN qty100  ELSE -qty100  END), 0) AS n100,
               COALESCE(SUM(CASE WHEN type = 'ADD' THEN qty50   ELSE -qty50   END), 0) AS n50,
               COALESCE(SUM(CASE WHEN type = 'ADD' THEN qty20   ELSE -qty20   END), 0) AS n20,
               COALESCE(SUM(CASE WHEN type = 'ADD' THEN qty10   ELSE -qty10   END), 0) AS n10,
               COALESCE(SUM(CASE WHEN type = 'ADD' THEN qty5    ELSE -qty5    END), 0) AS n5,
               COALESCE(SUM(CASE WHEN type = 'ADD' THEN qty2    ELSE -qty2    END), 0) AS n2,
               COALESCE(SUM(CASE WHEN type = 'ADD' THEN qty1    ELSE -qty1    END), 0) AS n1
        FROM transactions
    """)
    fun getNoteDistribution(): Flow<NoteDistribution>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    // ── Restore / backup helpers ───────────────────────────────────────────

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<Transaction>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<Transaction>): List<Long>

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @androidx.room.Transaction
    suspend fun replaceAll(transactions: List<Transaction>) {
        deleteAll()
        if (transactions.isNotEmpty()) insertAll(transactions)
    }
}

data class NoteDistribution(
    val n1000: Int = 0, val n500: Int = 0, val n200: Int = 0,
    val n100: Int = 0, val n50: Int = 0, val n20: Int = 0,
    val n10: Int = 0, val n5: Int = 0, val n2: Int = 0, val n1: Int = 0
) {
    fun totalNotes() = n1000 + n500 + n200 + n100 + n50 + n20 + n10 + n5 + n2 + n1

    fun asList(): List<Pair<Int, Int>> = listOf(
        1000 to n1000, 500 to n500, 200 to n200, 100 to n100, 50 to n50,
        20 to n20, 10 to n10, 5 to n5, 2 to n2, 1 to n1
    ).filter { it.second > 0 }
}