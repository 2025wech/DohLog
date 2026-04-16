package com.autoledger.di

import com.autoledger.data.parser.MpesaParser
import com.autoledger.data.parser.AirtelMoneyParser
import android.content.Context
import androidx.room.Room
import com.autoledger.data.local.dao.TransactionDao
import com.autoledger.data.local.db.AppDatabase
import com.autoledger.data.repository.TransactionRepositoryImpl
import com.autoledger.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "autoledger.db"
    ).build()

    @Provides
    @Singleton
    fun provideTransactionDao(database: AppDatabase): TransactionDao =
        database.transactionDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // Tells Hilt: when someone asks for TransactionRepository,
    // give them TransactionRepositoryImpl
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository
}
// Add this at the bottom of DatabaseModule.kt
// below the existing RepositoryModule

@Module
@InstallIn(SingletonComponent::class)
object ParserModule {

    @Provides
    @Singleton
    fun provideMpesaParser(): MpesaParser = MpesaParser()

    @Provides
    @Singleton
    fun provideAirtelMoneyParser(): AirtelMoneyParser = AirtelMoneyParser()
}