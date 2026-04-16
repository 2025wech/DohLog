package com.autoledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autoledger.domain.model.MobileMoneyProvider
import com.autoledger.domain.model.TransactionCategory
import com.autoledger.ui.*
import com.autoledger.ui.viewmodels.TransactionViewModel
import java.time.format.DateTimeFormatter

@Composable
fun TransactionDetailScreen(
    transactionId: String,
    viewModel: TransactionViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val transaction  = transactions.find { it.transactionId == transactionId }
        ?: return

    val isMpesa  = transaction.provider == MobileMoneyProvider.MPESA
    val isCredit = transaction.category == TransactionCategory.RECEIVE_MONEY
    val providerColor = if (isMpesa) SafaricomGreen else AirtelRed
    val amountColor   = if (isCredit) SafaricomGreen else ErrorRed
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d yyyy 'at' h:mm a")

    var showDeleteDialog by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor   = CardBlack,
            title = {
                Text("Delete transaction?",
                    color = TextPrimary, fontFamily = InterFontFamily)
            },
            text = {
                Text("This action cannot be undone.",
                    color = TextSecondary, fontFamily = InterFontFamily)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(transaction)
                    showDeleteDialog = false
                    onBack()
                }) {
                    Text("Delete", color = ErrorRed, fontFamily = InterFontFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary, fontFamily = InterFontFamily)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = TextPrimary)
            }
            Spacer(Modifier.weight(1f))
            Text("Transaction detail", fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary, fontFamily = InterFontFamily)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Rounded.Delete, "Delete", tint = ErrorRed)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount hero
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceBlack)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Provider badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(providerColor.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isMpesa) "M-Pesa" else "Airtel Money",
                        fontSize   = 11.sp,
                        color      = providerColor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFontFamily
                    )
                }
                Text(
                    "${if (isCredit) "+" else "−"} KES ${"%,.2f".format(transaction.amount)}",
                    fontSize   = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color      = amountColor,
                    fontFamily = InterFontFamily,
                    letterSpacing = (-1).sp
                )
                Text(
                    transaction.dateTime.format(dateFormatter),
                    fontSize = 13.sp,
                    color    = TextSecondary,
                    fontFamily = InterFontFamily
                )
            }

            // Details card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceBlack)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                DetailRow("Transaction ID", transaction.transactionId)
                HorizontalDivider(color = BorderSubtle,
                    modifier = Modifier.padding(vertical = 12.dp))
                DetailRow(
                    if (isCredit) "Received from" else "Sent to",
                    transaction.sender
                )
                HorizontalDivider(color = BorderSubtle,
                    modifier = Modifier.padding(vertical = 12.dp))
                DetailRow("Category", transaction.category.name
                    .replace("_", " ").lowercase()
                    .replaceFirstChar { it.uppercase() })
                HorizontalDivider(color = BorderSubtle,
                    modifier = Modifier.padding(vertical = 12.dp))
                DetailRow("Balance after", "KES ${"%,.2f".format(transaction.balance)}")
                HorizontalDivider(color = BorderSubtle,
                    modifier = Modifier.padding(vertical = 12.dp))
                DetailRow("Provider",
                    if (isMpesa) "M-Pesa (Safaricom)" else "Airtel Money")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary,
            fontFamily = InterFontFamily)
        Text(value, fontSize = 13.sp, color = TextPrimary,
            fontWeight = FontWeight.Medium, fontFamily = InterFontFamily)
    }
}