package com.autoledger.domain.usecase

import com.autoledger.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class MonthlySummary(
    val totalSent: Double,
    val totalReceived: Double,
    val balance: Double        // received - sent
)

class GetMonthlySummaryUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun getTotalSent(): Flow<Double> {
        val startOfMonth = getStartOfMonth()
        return repository.getTotalSentSince(startOfMonth)
    }

    fun getTotalReceived(): Flow<Double> {
        val startOfMonth = getStartOfMonth()
        return repository.getTotalReceivedSince(startOfMonth)
    }

    // Returns first day of current month as formatted string
    private fun getStartOfMonth(): String {
        val now = LocalDateTime.now()
        val startOfMonth = now.withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
        return startOfMonth.format(formatter)
    }
}