package com.autoledger.data.local.entity

import com.autoledger.domain.model.Transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

// Room Entity → Domain model (for UI)
fun TransactionEntity.toDomain(): Transaction = Transaction(
    transactionId = transactionId,
    amount        = amount,
    sender        = sender,
    dateTime      = LocalDateTime.parse(dateTime, formatter),
    balance       = balance,
    category      = category,
    provider      = provider
)

// Domain model → Room Entity (for saving)
fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    transactionId = transactionId,
    amount        = amount,
    sender        = sender,
    dateTime      = dateTime.format(formatter),
    balance       = balance,
    category      = category,
    provider      = provider
)