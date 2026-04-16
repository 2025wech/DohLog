package com.autoledger.data.local.dao

import androidx.room.*
import com.autoledger.data.local.entity.TransactionEntity
import com.autoledger.domain.model.MobileMoneyProvider
import com.autoledger.domain.model.TransactionCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    // ── Insert ────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    // ── All transactions ──────────────────────────────────────────────
    @Query("SELECT * FROM transactions ORDER BY dateTime DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    // ── Filter by provider ────────────────────────────────────────────
    @Query(
        "SELECT * FROM transactions " +
                "WHERE provider = :provider " +
                "ORDER BY dateTime DESC"
    )
    fun getByProvider(provider: MobileMoneyProvider): Flow<List<TransactionEntity>>

    // ── Filter by category ────────────────────────────────────────────
    @Query(
        "SELECT * FROM transactions " +
                "WHERE category = :category " +
                "ORDER BY dateTime DESC"
    )
    fun getByCategory(category: TransactionCategory): Flow<List<TransactionEntity>>

    // ── Monthly totals ────────────────────────────────────────────────
    @Query(
        "SELECT COALESCE(SUM(amount), 0.0) FROM transactions " +
                "WHERE category = 'SEND_MONEY' " +
                "AND dateTime >= :startOfMonth"
    )
    fun getTotalSentSince(startOfMonth: String): Flow<Double>

    @Query(
        "SELECT COALESCE(SUM(amount), 0.0) FROM transactions " +
                "WHERE category = 'RECEIVE_MONEY' " +
                "AND dateTime >= :startOfMonth"
    )
    fun getTotalReceivedSince(startOfMonth: String): Flow<Double>

    // ── Single transaction ────────────────────────────────────────────
    @Query("SELECT * FROM transactions WHERE transactionId = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    // ── Delete ────────────────────────────────────────────────────────
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    // ── Latest balance per provider ───────────────────────────────────
    // Returns the most recent transaction for a provider
    @Query(
        "SELECT * FROM transactions " +
                "WHERE provider = :provider " +
                "ORDER BY dateTime DESC " +
                "LIMIT 1"
    )
    suspend fun getLatestByProvider(
        provider: MobileMoneyProvider
    ): TransactionEntity?

    // Returns only the balance field from the most recent transaction
    // This is a Flow so the UI updates automatically when new SMS arrives
    @Query(
        "SELECT balance FROM transactions " +
                "WHERE provider = :provider " +
                "ORDER BY dateTime DESC " +
                "LIMIT 1"
    )
    fun getLatestBalanceByProvider(
        provider: MobileMoneyProvider
    ): Flow<Double?>
}