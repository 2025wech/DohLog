package com.autoledger.domain.usecase

import com.autoledger.domain.model.MobileMoneyProvider
import com.autoledger.domain.model.Transaction
import com.autoledger.domain.model.TransactionCategory
import com.autoledger.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FilterTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    fun byProvider(provider: MobileMoneyProvider): Flow<List<Transaction>> =
        repository.getByProvider(provider)

    fun byCategory(category: TransactionCategory): Flow<List<Transaction>> =
        repository.getByCategory(category)
}