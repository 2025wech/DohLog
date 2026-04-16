package com.autoledger.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.autoledger.domain.model.MobileMoneyProvider
import com.autoledger.domain.model.Transaction
import com.autoledger.domain.model.TransactionCategory
import com.autoledger.ui.*
import com.autoledger.ui.viewmodels.TransactionViewModel
import java.time.format.DateTimeFormatter
import com.autoledger.ui.viewmodels.AuthViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale



@Composable
fun DashboardScreen(
    viewModel: TransactionViewModel = hiltViewModel(),
    authViewModel : AuthViewModel        = hiltViewModel(),
    onTransactionClick: (String) -> Unit = {}
) {
    val userPrefs by authViewModel.userPreferences.collectAsState()
    val context       = LocalContext.current
    val transactions  by viewModel.transactions.collectAsState()
    val totalSent     by viewModel.totalSent.collectAsState()
    val totalReceived by viewModel.totalReceived.collectAsState()
    val uiState       by viewModel.uiState.collectAsState()
    val mpesaBalance  by viewModel.mpesaBalance.collectAsState()
    val airtelBalance by viewModel.airtelBalance.collectAsState()
    LaunchedEffect(airtelBalance) {
        android.util.Log.d("AIRTEL_BALANCE_UI",
            "Airtel balance from ViewModel: $airtelBalance")
    }

    var activeFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "M-Pesa", "Airtel", "Send", "Receive", "Bills")

    LaunchedEffect(Unit) {
        val hasPermission =
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission && transactions.isEmpty()) {
            viewModel.importSmsHistory()
        }
    }

    val filtered = remember(transactions, activeFilter) {
        when (activeFilter) {
            "M-Pesa"  -> transactions.filter {
                it.provider == MobileMoneyProvider.MPESA }
            "Airtel"  -> transactions.filter {
                it.provider == MobileMoneyProvider.AIRTEL_MONEY }
            "Send"    -> transactions.filter {
                it.category == TransactionCategory.SEND_MONEY }
            "Receive" -> transactions.filter {
                it.category == TransactionCategory.RECEIVE_MONEY }
            "Bills"   -> transactions.filter {
                it.category == TransactionCategory.PAY_BILL ||
                        it.category == TransactionCategory.BUY_GOODS }
            else -> transactions
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Update the header item call
        item {
            DashboardHeader(
                name      = userPrefs.userName,
                photoUri  = userPrefs.photoUri,   // ← add
                isLoading = uiState.isLoading
            )
        }
        item {
            BalanceCard(
                mpesaBalance  = mpesaBalance,
                airtelBalance = airtelBalance,
                totalSent     = totalSent,
                totalReceived = totalReceived,
                modifier      = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        if (uiState.isLoading) {
            item { ImportingBanner() }
        }
        if (uiState.importCount > 0 && !uiState.isLoading) {
            item {
                ImportSuccessBanner(
                    count     = uiState.importCount,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
        uiState.errorMessage?.let { error ->
            item {
                ErrorBanner(
                    message   = error,
                    onDismiss = { viewModel.clearError() }
                )
            }
        }
        item {
            QuickActionsRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
        item {
            FilterPillRow(
                filters          = filters,
                activeFilter     = activeFilter,
                onFilterSelected = { activeFilter = it }
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Recent transactions",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    fontFamily = InterFontFamily
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(SafaricomGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${filtered.size}",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = SafaricomGreen,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
        if (filtered.isEmpty() && !uiState.isLoading) {
            item { EmptyState(activeFilter = activeFilter) }
        }
        items(filtered, key = { it.transactionId }) { transaction ->
            TransactionItem(
                transaction = transaction,
                onClick     = { onTransactionClick(transaction.transactionId) },
                modifier    = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────
@Composable
private fun DashboardHeader(
    name      : String,
    photoUri  : String,     // ← add
    isLoading : Boolean
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                "Good morning,",
                fontSize   = 10.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily
            )
            Text(
                name.ifBlank { "there" },
                fontSize      = 15.sp,
                fontWeight    = FontWeight.Bold,
                color         = TextPrimary,
                fontFamily    = InterFontFamily,
                letterSpacing = (-0.2).sp
            )
        }
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color       = SafaricomGreen,
                    modifier    = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp
                )
            }
            // Avatar — shows photo or initial
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(SafaricomGreen),
                contentAlignment = Alignment.Center
            ) {
                if (photoUri.isNotEmpty()) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(android.net.Uri.parse(photoUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile photo",
                        modifier           = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Text(
                        name.firstOrNull()?.uppercaseChar()
                            ?.toString() ?: "A",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TrueBlack,
                        fontFamily = InterFontFamily
                    )
                }
            }
        }
    }
}

// ── Balance Card ──────────────────────────────────────────────────────────
@Composable
private fun BalanceCard(
    mpesaBalance  : Double?,
    airtelBalance : Double?,
    totalSent     : Double,
    totalReceived : Double,
    modifier      : Modifier = Modifier
) {
    // Combined balance = sum of both provider balances
    val combinedBalance = (mpesaBalance ?: 0.0) + (airtelBalance ?: 0.0)

    Column(
        modifier            = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Combined total balance ────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0D1F0D))
                .border(
                    width = 1.dp,
                    color = SafaricomGreen.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Total balance",
                    fontSize      = 9.sp,
                    color         = SafaricomGreen,
                    fontWeight    = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                    fontFamily    = InterFontFamily
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "KES ${"%,.2f".format(combinedBalance)}",
                    fontSize      = 20.sp,
                    fontWeight    = FontWeight.Bold,
                    color         = if (combinedBalance >= 0)
                        TextPrimary else ErrorRed,
                    letterSpacing = (-0.5).sp,
                    fontFamily    = InterFontFamily
                )
                Text(
                    "M-Pesa + Airtel Money combined",
                    fontSize   = 9.sp,
                    color      = TextMuted,
                    fontFamily = InterFontFamily
                )

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Spacer(Modifier.height(10.dp))

                // Monthly sent / received
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier            = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            "Spent this month",
                            fontSize   = 9.sp,
                            color      = TextSecondary,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            "−KES ${"%,.0f".format(totalSent)}",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color      = ErrorRed,
                            fontFamily = InterFontFamily
                        )
                    }
                    Column(
                        modifier            = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            "Received this month",
                            fontSize   = 9.sp,
                            color      = TextSecondary,
                            fontFamily = InterFontFamily
                        )
                        Text(
                            "+KES ${"%,.0f".format(totalReceived)}",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color      = SafaricomGreen,
                            fontFamily = InterFontFamily
                        )
                    }
                }
            }
        }

        // ── Individual provider balances ──────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // M-Pesa balance card
            ProviderBalanceCard(
                providerName  = "M-Pesa",
                providerColor = SafaricomGreen,
                balance       = mpesaBalance,
                initial       = "M",
                modifier      = Modifier.weight(1f)
            )

            // Airtel Money balance card
            ProviderBalanceCard(
                providerName  = "Airtel Money",
                providerColor = AirtelRed,
                balance       = airtelBalance,
                initial       = "A",
                modifier      = Modifier.weight(1f)
            )
        }
    }
}

// ── Provider balance card ─────────────────────────────────────────────────
@Composable
private fun ProviderBalanceCard(
    providerName  : String,
    providerColor : Color,
    balance       : Double?,
    initial       : String,
    modifier      : Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceBlack)
            .border(
                width = 1.dp,
                color = providerColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Provider label row
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(providerColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    initial,
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color      = providerColor,
                    fontFamily = InterFontFamily
                )
            }
            Text(
                providerName,
                fontSize   = 10.sp,
                color      = providerColor,
                fontWeight = FontWeight.SemiBold,
                fontFamily = InterFontFamily
            )
        }

        // Balance amount
        if (balance != null) {
            Text(
                "KES ${"%,.2f".format(balance)}",
                fontSize      = 14.sp,
                fontWeight    = FontWeight.Bold,
                color         = TextPrimary,
                fontFamily    = InterFontFamily,
                letterSpacing = (-0.3).sp
            )
            Text(
                "Last SMS balance",
                fontSize   = 8.sp,
                color      = TextMuted,
                fontFamily = InterFontFamily
            )
        } else {
            Text(
                "No data",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = TextMuted,
                fontFamily = InterFontFamily
            )
            Text(
                "No ${providerName} SMS found",
                fontSize   = 8.sp,
                color      = TextMuted,
                fontFamily = InterFontFamily
            )
        }
    }
}

// ── Importing Banner ──────────────────────────────────────────────────────
@Composable
private fun ImportingBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SafaricomGreen.copy(alpha = 0.08f))
            .border(1.dp, SafaricomGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator(
            color       = SafaricomGreen,
            modifier    = Modifier.size(11.dp),
            strokeWidth = 1.5.dp
        )
        Text(
            "Reading your M-Pesa & Airtel SMS history...",
            fontSize   = 11.sp,
            color      = SafaricomGreen,
            fontFamily = InterFontFamily
        )
    }
}

// ── Import Success Banner ─────────────────────────────────────────────────
@Composable
private fun ImportSuccessBanner(count: Int, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SafaricomGreen.copy(alpha = 0.08f))
            .border(1.dp, SafaricomGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(SafaricomGreen)
        )
        Text(
            "Imported $count transactions",
            fontSize   = 11.sp,
            color      = SafaricomGreen,
            fontFamily = InterFontFamily,
            modifier   = Modifier.weight(1f)
        )
        TextButton(
            onClick        = onDismiss,
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                "Dismiss",
                fontSize   = 10.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily
            )
        }
    }
}

// ── Error Banner ──────────────────────────────────────────────────────────
@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ErrorRed.copy(alpha = 0.08f))
            .border(1.dp, ErrorRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            message,
            fontSize   = 11.sp,
            color      = ErrorRed,
            fontFamily = InterFontFamily,
            modifier   = Modifier.weight(1f)
        )
        TextButton(
            onClick        = onDismiss,
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                "Dismiss",
                fontSize   = 10.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily
            )
        }
    }
}

// ── Quick Actions ─────────────────────────────────────────────────────────
@Composable
private fun QuickActionsRow(modifier: Modifier = Modifier) {
    val actions = listOf(
        Pair("Send",     "↑"),
        Pair("Receive",  "↓"),
        Pair("Pay bill", "\uD83D\uDCB3"),
        Pair("History",  "\uD83D\uDD52")
    )
    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        actions.forEach { (label, icon) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceBlack)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SafaricomGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        icon,
                        color      = SafaricomGreen,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    label,
                    fontSize   = 8.sp,
                    color      = TextSecondary,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Filter Pills ──────────────────────────────────────────────────────────
@Composable
private fun FilterPillRow(
    filters: List<String>,
    activeFilter: String,
    onFilterSelected: (String) -> Unit
) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(filters) { filter ->
            val isActive = filter == activeFilter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isActive) SafaricomGreen else SurfaceBlack)
                    .border(
                        width = 1.dp,
                        color = if (isActive) SafaricomGreen else BorderSubtle,
                        shape = RoundedCornerShape(100.dp)
                    )
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    filter,
                    fontSize   = 10.sp,
                    color      = if (isActive) TrueBlack else TextSecondary,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

// ── Transaction Item ──────────────────────────────────────────────────────
@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter     = remember { DateTimeFormatter.ofPattern("MMM d, h:mm a") }
    val isMpesa       = transaction.provider == MobileMoneyProvider.MPESA
    val isCredit      = transaction.category == TransactionCategory.RECEIVE_MONEY ||
            transaction.category == TransactionCategory.DEPOSIT
    val providerColor = if (isMpesa) SafaricomGreen else AirtelRed
    val amountColor   = if (isCredit) SafaricomGreen else ErrorRed
    val amountPrefix  = if (isCredit) "+" else "−"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceBlack)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(9.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(providerColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isCredit) "↓" else "↑",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = providerColor
            )
        }

        // Info
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                transaction.sender,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
                fontFamily = InterFontFamily,
                maxLines   = 1
            )
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(providerColor)
                )
                Text(
                    if (isMpesa) "M-Pesa" else "Airtel",
                    fontSize   = 9.sp,
                    color      = TextMuted,
                    fontFamily = InterFontFamily
                )
                CategoryBadge(transaction.category)
            }
        }

        // Amount + time
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "$amountPrefix KES ${"%,.0f".format(transaction.amount)}",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                color      = amountColor,
                fontFamily = InterFontFamily
            )
            Spacer(Modifier.height(1.dp))
            Text(
                transaction.dateTime.format(formatter),
                fontSize   = 8.sp,
                color      = TextMuted,
                fontFamily = InterFontFamily
            )
        }
    }
}

// ── Category Badge ────────────────────────────────────────────────────────
@Composable
private fun CategoryBadge(category: TransactionCategory) {
    val (label, bg, fg) = when (category) {
        TransactionCategory.RECEIVE_MONEY ->
            Triple("Received",  SafaricomGreen.copy(.12f), SafaricomGreen)
        TransactionCategory.SEND_MONEY ->
            Triple("Send",      ErrorRed.copy(.12f),       ErrorRed)
        TransactionCategory.BUY_GOODS ->
            Triple("Buy goods", Color(0x1A8888FF),         Color(0xFF8888FF))
        TransactionCategory.PAY_BILL ->
            Triple("Bill",      Color(0x1AFFA040),         Color(0xFFFFA040))
        TransactionCategory.AIRTIME ->
            Triple("Airtime",   Color(0x1AFFA040),         Color(0xFFFFA040))
        TransactionCategory.WITHDRAWAL ->
            Triple("Withdraw",  ErrorRed.copy(.12f),       ErrorRed)
        TransactionCategory.DEPOSIT ->
            Triple("Deposit",   SafaricomGreen.copy(.12f), SafaricomGreen)
        else ->
            Triple("Other",     Color(0x1A888888),         TextSecondary)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(bg)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            label.uppercase(),
            fontSize      = 7.sp,
            color         = fg,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
            fontFamily    = InterFontFamily
        )
    }
}

// ── Empty State ───────────────────────────────────────────────────────────
@Composable
private fun EmptyState(activeFilter: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SurfaceBlack)
                .border(1.dp, BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("◎", fontSize = 18.sp, color = TextMuted)
        }
        Spacer(Modifier.height(3.dp))
        Text(
            if (activeFilter == "All") "No transactions yet"
            else "No $activeFilter transactions",
            fontSize   = 13.sp,
            color      = TextSecondary,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium
        )
        Text(
            if (activeFilter == "All")
                "Your M-Pesa & Airtel SMS\nwill appear here automatically"
            else "Try a different filter",
            fontSize   = 11.sp,
            color      = TextMuted,
            fontFamily = InterFontFamily,
            textAlign  = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}