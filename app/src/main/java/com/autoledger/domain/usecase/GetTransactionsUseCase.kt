package com.autoledger.domain.usecase

import com.autoledger.domain.model.Transaction
import com.autoledger.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    // Returns live stream of all transactions
    operator fun invoke(): Flow<List<Transaction>> =
        repository.getAllTransactions()
}