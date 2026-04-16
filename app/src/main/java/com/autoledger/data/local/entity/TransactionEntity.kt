package com.autoledger.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.autoledger.domain.model.MobileMoneyProvider
import com.autoledger.domain.model.TransactionCategory

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val transactionId: String,
    val amount: Double,
    val sender: String,
    val dateTime: String,       // stored as "2024-09-15T14:30:00"
    val balance: Double,
    val category: TransactionCategory,
    val provider: MobileMoneyProvider
)