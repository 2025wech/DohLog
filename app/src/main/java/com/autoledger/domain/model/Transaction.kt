package com.autoledger.domain.model

import java.time.LocalDateTime

data class Transaction(
    val transactionId: String,
    val amount: Double,
    val sender: String,
    val dateTime: LocalDateTime,
    val balance: Double,
    val category: TransactionCategory,
    val provider: MobileMoneyProvider
)