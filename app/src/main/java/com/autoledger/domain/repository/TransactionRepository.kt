package com.autoledger.domain.repository

import com.autoledger.domain.model.MobileMoneyProvider
import com.autoledger.domain.model.Transaction
import com.autoledger.domain.model.TransactionCategory
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    // Observe all transactions (live, updates UI automatically)
    fun getAllTransactions(): Flow<List<Transaction>>

    // Observe by provider
    fun getByProvider(provider: MobileMoneyProvider): Flow<List<Transaction>>

    // Observe by category
    fun getByCategory(category: TransactionCategory): Flow<List<Transaction>>

    // Total sent since a date (for dashboard summary)
    fun getTotalSentSince(startOfMonth: String): Flow<Double>

    // Total received since a date
    fun getTotalReceivedSince(startOfMonth: String): Flow<Double>

    // Save a single new transaction (called from SMS parser)
    suspend fun saveTransaction(transaction: Transaction)

    // Save a batch (called on first launch to import SMS history)
    suspend fun saveAll(transactions: List<Transaction>)

    // Get one transaction by ID (for detail screen)
    suspend fun getTransactionById(id: String): Transaction?

    // Delete one
    suspend fun deleteTransaction(transaction: Transaction)
    // Latest balance from most recent transaction per provider
    fun getMpesaBalance(): Flow<Double?>
    fun getAirtelBalance(): Flow<Double?>
    suspend fun clearAll()
}