package com.autoledger.domain.usecase

import com.autoledger.domain.model.Transaction
import com.autoledger.domain.repository.TransactionRepository
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) =
        repository.deleteTransaction(transaction)
}