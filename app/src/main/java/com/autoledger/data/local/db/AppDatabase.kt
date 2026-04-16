package com.autoledger.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.autoledger.data.local.converter.Converters
import com.autoledger.data.local.dao.TransactionDao
import com.autoledger.data.local.entity.TransactionEntity

@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
}