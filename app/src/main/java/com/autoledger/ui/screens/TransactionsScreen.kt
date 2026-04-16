package com.autoledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autoledger.ui.*
import com.autoledger.ui.viewmodels.TransactionViewModel
import androidx.compose.foundation.ExperimentalFoundationApi // Required for stickyHeader
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    onTransactionClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()

    // 1. Group transactions by date
    val groupedTransactions = remember(transactions) {
        val formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
        transactions.groupBy { tx ->
            tx.dateTime.format(formatter)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
    ) {
        // ... (Your existing Top Bar code stays the same)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(
                "All transactions",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                fontFamily = InterFontFamily
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .background(SafaricomGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    "${transactions.size}",
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color      = SafaricomGreen,
                    fontFamily = InterFontFamily
                )
            }
        }

        HorizontalDivider(color = BorderSubtle)

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No transactions found", color = TextSecondary, fontFamily = InterFontFamily)
            }
        } else {
            // 2. Updated LazyColumn with Headers
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupedTransactions.forEach { (date, transactionsInDay) ->

                    // The "Bulk" Header
                    stickyHeader {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(TrueBlack) // Matches your background
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = date,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                fontFamily = InterFontFamily,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    // The actual transaction items for that day
                    items(transactionsInDay, key = { it.transactionId }) { tx ->
                        TransactionItem(
                            transaction = tx,
                            onClick     = { onTransactionClick(tx.transactionId) }
                        )
                    }

                    // Optional: Add extra space after each day bulk
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}