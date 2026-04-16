package com.autoledger.domain.usecase

import com.autoledger.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProviderBalancesUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    fun getMpesaBalance(): Flow<Double?> =
        repository.getMpesaBalance()

    fun getAirtelBalance(): Flow<Double?> =
        repository.getAirtelBalance()
}