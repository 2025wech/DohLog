package com.autoledger.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autoledger.data.receiver.SmsImporter
import com.autoledger.domain.model.MobileMoneyProvider
import com.autoledger.domain.model.Transaction
import com.autoledger.domain.model.TransactionCategory
import com.autoledger.domain.usecase.ClearAllTransactionsUseCase
import com.autoledger.domain.usecase.DeleteTransactionUseCase
import com.autoledger.domain.usecase.FilterTransactionsUseCase
import com.autoledger.domain.usecase.GetMonthlySummaryUseCase
import com.autoledger.domain.usecase.GetProviderBalancesUseCase
import com.autoledger.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionUiState(
    val transactions  : List<Transaction> = emptyList(),
    val totalSent     : Double            = 0.0,
    val totalReceived : Double            = 0.0,
    val isLoading     : Boolean           = false,
    val importCount   : Int               = 0,
    val errorMessage  : String?           = null
)

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val getTransactions     : GetTransactionsUseCase,
    private val getMonthlySummary   : GetMonthlySummaryUseCase,
    private val filterTransactions  : FilterTransactionsUseCase,
    private val deleteTransaction   : DeleteTransactionUseCase,
    private val clearAllTransactions: ClearAllTransactionsUseCase,
    private val getProviderBalances  : GetProviderBalancesUseCase,
    private val smsImporter         : SmsImporter
) : ViewModel() {

    // ── UI state ──────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow(TransactionUiState())
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    // ── All transactions ──────────────────────────────────────────────
    val transactions: StateFlow<List<Transaction>> =
        getTransactions()
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // ── Monthly totals ────────────────────────────────────────────────
    val totalSent: StateFlow<Double> =
        getMonthlySummary.getTotalSent()
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0.0
            )

    val totalReceived: StateFlow<Double> =
        getMonthlySummary.getTotalReceived()
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = 0.0
            )

    // ── Provider balances ─────────────────────────────────────────────
    val mpesaBalance: StateFlow<Double?> =
        getProviderBalances.getMpesaBalance()
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    val airtelBalance: StateFlow<Double?> =
        getProviderBalances.getAirtelBalance()
            .stateIn(
                scope        = viewModelScope,
                started      = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )

    // ── Filtered transactions ─────────────────────────────────────────
    private val _filteredTransactions =
        MutableStateFlow<List<Transaction>>(emptyList())
    val filteredTransactions: StateFlow<List<Transaction>> =
        _filteredTransactions.asStateFlow()

    fun filterByProvider(provider: MobileMoneyProvider) {
        viewModelScope.launch {
            filterTransactions.byProvider(provider).collect { list ->
                _filteredTransactions.update { list }
            }
        }
    }

    fun filterByCategory(category: TransactionCategory) {
        viewModelScope.launch {
            filterTransactions.byCategory(category).collect { list ->
                _filteredTransactions.update { list }
            }
        }
    }

    // ── Delete single ─────────────────────────────────────────────────
    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            deleteTransaction(transaction)
        }
    }

    // ── SMS Import ────────────────────────────────────────────────────
    fun importSmsHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val count = smsImporter.importAll()
                _uiState.update {
                    it.copy(isLoading = false, importCount = count)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        errorMessage = "Import failed: ${e.message}"
                    )
                }
            }
        }
    }

    // ── Clear all + re-import ─────────────────────────────────────────
    fun clearAndReimport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                clearAllTransactions()          // ← use case, no repository ref needed
                val count = smsImporter.importAll()
                _uiState.update {
                    it.copy(isLoading = false, importCount = count)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading    = false,
                        errorMessage = "Re-import failed: ${e.message}"
                    )
                }
            }
        }
    }

    // ── Clear error ───────────────────────────────────────────────────
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}