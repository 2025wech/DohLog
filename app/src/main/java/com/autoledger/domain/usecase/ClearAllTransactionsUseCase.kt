package com.autoledger.domain.usecase

import com.autoledger.domain.repository.TransactionRepository
import javax.inject.Inject

class ClearAllTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke() = repository.clearAll()
}