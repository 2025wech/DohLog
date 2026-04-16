package com.autoledger.data.repository

import com.autoledger.data.local.dao.TransactionDao
import com.autoledger.data.local.entity.toDomain
import com.autoledger.data.local.entity.toEntity
import com.autoledger.domain.model.MobileMoneyProvider
import com.autoledger.domain.model.Transaction
import com.autoledger.domain.model.TransactionCategory
import com.autoledger.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> =
        dao.getAllTransactions().map { list ->
            list.map { it.toDomain() }
        }

    override fun getByProvider(provider: MobileMoneyProvider): Flow<List<Transaction>> =
        dao.getByProvider(provider).map { list ->
            list.map { it.toDomain() }
        }

    override fun getByCategory(category: TransactionCategory): Flow<List<Transaction>> =
        dao.getByCategory(category).map { list ->
            list.map { it.toDomain() }
        }

    override fun getTotalSentSince(startOfMonth: String): Flow<Double> =
        dao.getTotalSentSince(startOfMonth)

    override fun getTotalReceivedSince(startOfMonth: String): Flow<Double> =
        dao.getTotalReceivedSince(startOfMonth)

    override suspend fun saveTransaction(transaction: Transaction) =
        dao.insertTransaction(transaction.toEntity())

    override suspend fun saveAll(transactions: List<Transaction>) =
        dao.insertAll(transactions.map { it.toEntity() })

    override suspend fun getTransactionById(id: String): Transaction? =
        dao.getTransactionById(id)?.toDomain()

    override suspend fun deleteTransaction(transaction: Transaction) =
        dao.deleteTransaction(transaction.toEntity())
    override fun getMpesaBalance(): Flow<Double?> =
        dao.getLatestBalanceByProvider(MobileMoneyProvider.MPESA)

    override fun getAirtelBalance(): Flow<Double?> =
        dao.getLatestBalanceByProvider(MobileMoneyProvider.AIRTEL_MONEY)
            .also {
                android.util.Log.d("AIRTEL_BALANCE_DB",
                    "Querying Airtel balance from DB")
            }
    override suspend fun clearAll() = dao.deleteAll()
}
