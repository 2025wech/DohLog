package com.autoledger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.autoledger.domain.model.Transaction
import com.autoledger.domain.model.TransactionCategory
import com.autoledger.ui.*
import com.autoledger.ui.viewmodels.TransactionViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class Period { DAILY, WEEKLY, MONTHLY }

@Composable
fun AnalyticsScreen(
    viewModel : TransactionViewModel = hiltViewModel(),
    onBack    : () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    var activePeriod by remember { mutableStateOf(Period.MONTHLY) }
    val now          = remember { LocalDateTime.now() }

    // ── Re-filter whenever period or transactions change ──────────────
    val periodTransactions by remember(transactions, activePeriod) {
        derivedStateOf {
            val cutoff = when (activePeriod) {
                Period.DAILY   -> now.minusHours(24)
                Period.WEEKLY  -> now.minusDays(7)
                Period.MONTHLY -> now.minusDays(30)
            }
            transactions.filter { it.dateTime.isAfter(cutoff) }
        }
    }

    val totalIn by remember(periodTransactions) {
        derivedStateOf {
            periodTransactions.filter {
                it.category == TransactionCategory.RECEIVE_MONEY ||
                        it.category == TransactionCategory.DEPOSIT
            }.sumOf { it.amount }
        }
    }

    val totalOut by remember(periodTransactions) {
        derivedStateOf {
            periodTransactions.filter {
                it.category != TransactionCategory.RECEIVE_MONEY &&
                        it.category != TransactionCategory.DEPOSIT
            }.sumOf { it.amount }
        }
    }

    val net by remember(totalIn, totalOut) {
        derivedStateOf { totalIn - totalOut }
    }

    val categoryTotals by remember(periodTransactions) {
        derivedStateOf {
            periodTransactions
                .groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
        }
    }

    val maxCategoryAmount by remember(categoryTotals) {
        derivedStateOf {
            categoryTotals.maxOfOrNull { it.second } ?: 1.0
        }
    }

    val timeSeriesData by remember(periodTransactions, activePeriod) {
        derivedStateOf {
            buildTimeSeriesData(periodTransactions, activePeriod, now)
        }
    }

    val maxBarAmount by remember(timeSeriesData) {
        derivedStateOf {
            timeSeriesData.maxOfOrNull { it.second }
                .takeIf { it != null && it > 0.0 } ?: 1.0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TrueBlack)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp)
    ) {
        // ── Top bar ───────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint               = TextPrimary,
                    modifier           = Modifier.size(20.dp)
                )
            }
            Text(
                "Analytics",
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                fontFamily = InterFontFamily
            )
        }

        Column(
            modifier            = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Period pills ──────────────────────────────────────────
            PeriodSelector(
                activePeriod = activePeriod,
                onSelect     = { activePeriod = it }   // ← this triggers recompose
            )

            // ── Summary cards ─────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(
                    label    = "Money in",
                    amount   = totalIn,
                    color    = SafaricomGreen,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    label    = "Money out",
                    amount   = totalOut,
                    color    = ErrorRed,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    label    = "Net",
                    amount   = net,
                    color    = if (net >= 0) SafaricomGreen else ErrorRed,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Expenditure bar chart ─────────────────────────────────
            SectionCard(
                title = "Expenditure — ${periodLabel(activePeriod)}"
            ) {
                if (timeSeriesData.all { it.second == 0.0 }) {
                    EmptyChartState()
                } else {
                    ExpenditureBarChart(
                        data      = timeSeriesData,
                        maxAmount = maxBarAmount
                    )
                }
            }

            // ── Mini stats ────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniStatCard(
                    label    = "Transactions",
                    value    = "${periodTransactions.size}",
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    label    = "Avg amount",
                    value    = if (periodTransactions.isEmpty()) "KES 0"
                    else "KES ${
                        "%,.0f".format(
                            periodTransactions.sumOf { it.amount } /
                                    periodTransactions.size
                        )
                    }",
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Category breakdown ────────────────────────────────────
            SectionCard(title = "Breakdown by category") {
                if (categoryTotals.isEmpty()) {
                    EmptyChartState()
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        categoryTotals.forEach { (category, amount) ->
                            CategoryBarRow(
                                label     = category.name
                                    .replace("_", " ")
                                    .lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                amount    = amount,
                                maxAmount = maxCategoryAmount,
                                color     = categoryColor(category)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Period selector ───────────────────────────────────────────────────────
@Composable
private fun PeriodSelector(
    activePeriod : Period,
    onSelect     : (Period) -> Unit
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Period.entries.forEach { period ->
            val isActive      = period == activePeriod
            val label         = when (period) {
                Period.DAILY   -> "24 hrs"
                Period.WEEKLY  -> "7 days"
                Period.MONTHLY -> "30 days"
            }
            val interactionSource = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        if (isActive) SafaricomGreen else SurfaceBlack
                    )
                    .border(
                        width = 1.dp,
                        color = if (isActive) SafaricomGreen else BorderSubtle,
                        shape = RoundedCornerShape(100.dp)
                    )
                    .clickable(                  // ← was missing before
                        interactionSource = interactionSource,
                        indication        = null
                    ) {
                        onSelect(period)         // ← triggers state change
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = label,
                    fontSize   = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold
                    else FontWeight.Normal,
                    color      = if (isActive) TrueBlack else TextSecondary,
                    fontFamily = InterFontFamily
                )
            }
        }
    }
}

// ── Expenditure bar chart ─────────────────────────────────────────────────
@Composable
private fun ExpenditureBarChart(
    data      : List<Pair<String, Double>>,
    maxAmount : Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment     = Alignment.Bottom
        ) {
            data.forEach { (_, amount) ->
                val fraction = if (maxAmount > 0)
                    (amount / maxAmount).toFloat().coerceIn(0f, 1f)
                else 0f

                Column(
                    modifier            = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Amount label above bar
                    if (fraction > 0.12f) {
                        Text(
                            text       = shortAmount(amount),
                            fontSize   = 7.sp,
                            color      = SafaricomGreen,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            textAlign  = TextAlign.Center,
                            maxLines   = 1
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    // Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                            .clip(
                                RoundedCornerShape(
                                    topStart = 3.dp,
                                    topEnd   = 3.dp
                                )
                            )
                            .background(
                                if (amount > 0)
                                    SafaricomGreen.copy(alpha = 0.85f)
                                else
                                    BorderSubtle
                            )
                    )
                }
            }
        }

        // X-axis labels
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            data.forEach { (label, _) ->
                Text(
                    text       = label,
                    fontSize   = 7.5.sp,
                    color      = TextMuted,
                    fontFamily = InterFontFamily,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.weight(1f),
                    maxLines   = 1
                )
            }
        }
    }
}

// ── Category bar row ──────────────────────────────────────────────────────
@Composable
private fun CategoryBarRow(
    label     : String,
    amount    : Double,
    maxAmount : Double,
    color     : Color
) {
    val progress = (amount / maxAmount).toFloat().coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontSize   = 11.sp,
                color      = TextPrimary,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            )
            Text(
                "KES ${"%,.0f".format(amount)}",
                fontSize   = 11.sp,
                color      = TextSecondary,
                fontFamily = InterFontFamily
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(100))
                .background(BorderSubtle)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100))
                    .background(color)
            )
        }
    }
}

// ── Section card ──────────────────────────────────────────────────────────
@Composable
private fun SectionCard(
    title   : String,
    content : @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceBlack)
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            title,
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary,
            fontFamily = InterFontFamily
        )
        content()
    }
}

// ── Summary card ──────────────────────────────────────────────────────────
@Composable
private fun SummaryCard(
    label    : String,
    amount   : Double,
    color    : Color,
    modifier : Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceBlack)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            label,
            fontSize   = 9.sp,
            color      = TextSecondary,
            fontFamily = InterFontFamily
        )
        Text(
            "KES ${"%,.0f".format(Math.abs(amount))}",
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            color      = color,
            fontFamily = InterFontFamily,
            maxLines   = 1
        )
    }
}

// ── Mini stat card ────────────────────────────────────────────────────────
@Composable
private fun MiniStatCard(
    label    : String,
    value    : String,
    modifier : Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceBlack)
            .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            label,
            fontSize   = 9.sp,
            color      = TextSecondary,
            fontFamily = InterFontFamily
        )
        Text(
            value,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            fontFamily = InterFontFamily,
            maxLines   = 1
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────
@Composable
private fun EmptyChartState() {
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BorderSubtle),
                contentAlignment = Alignment.Center
            ) {
                Text("◎", fontSize = 14.sp, color = TextMuted)
            }
            Text(
                "No data for this period",
                fontSize   = 11.sp,
                color      = TextMuted,
                fontFamily = InterFontFamily
            )
        }
    }
}

// ── Time series builder ───────────────────────────────────────────────────
private fun buildTimeSeriesData(
    transactions : List<Transaction>,
    period       : Period,
    now          : LocalDateTime
): List<Pair<String, Double>> {
    return when (period) {

        // 24 hrs — last 12 hours grouped by hour
        Period.DAILY -> {
            val fmt = DateTimeFormatter.ofPattern("ha")
            (11 downTo 0).map { hoursAgo ->
                val hour  = now.minusHours(hoursAgo.toLong())
                val label = hour.format(fmt)
                val total = transactions.filter {
                    it.dateTime.year       == hour.year       &&
                            it.dateTime.month      == hour.month      &&
                            it.dateTime.dayOfMonth == hour.dayOfMonth &&
                            it.dateTime.hour       == hour.hour       &&
                            it.category != TransactionCategory.RECEIVE_MONEY &&
                            it.category != TransactionCategory.DEPOSIT
                }.sumOf { it.amount }
                Pair(label, total)
            }
        }

        // 7 days — last 7 days grouped by day
        Period.WEEKLY -> {
            val fmt = DateTimeFormatter.ofPattern("EEE")
            (6 downTo 0).map { daysAgo ->
                val day   = now.minusDays(daysAgo.toLong())
                val label = if (daysAgo == 0) "Today" else day.format(fmt)
                val total = transactions.filter {
                    it.dateTime.year       == day.year       &&
                            it.dateTime.month      == day.month      &&
                            it.dateTime.dayOfMonth == day.dayOfMonth &&
                            it.category != TransactionCategory.RECEIVE_MONEY &&
                            it.category != TransactionCategory.DEPOSIT
                }.sumOf { it.amount }
                Pair(label, total)
            }
        }

        // 30 days — last 4 weeks grouped by week
        Period.MONTHLY -> {
            (3 downTo 0).map { weeksAgo ->
                val weekEnd   = now.minusWeeks(weeksAgo.toLong())
                val weekStart = now.minusWeeks((weeksAgo + 1).toLong())
                val label     = when (weeksAgo) {
                    0    -> "This wk"
                    1    -> "Last wk"
                    2    -> "2 wks"
                    else -> "3 wks"
                }
                val total = transactions.filter {
                    it.dateTime.isAfter(weekStart) &&
                            it.dateTime.isBefore(weekEnd)  &&
                            it.category != TransactionCategory.RECEIVE_MONEY &&
                            it.category != TransactionCategory.DEPOSIT
                }.sumOf { it.amount }
                Pair(label, total)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────
private fun periodLabel(period: Period) = when (period) {
    Period.DAILY   -> "last 24 hours"
    Period.WEEKLY  -> "last 7 days"
    Period.MONTHLY -> "last 30 days"
}

private fun shortAmount(amount: Double): String = when {
    amount >= 1_000_000 -> "${"%.1f".format(amount / 1_000_000)}M"
    amount >= 1_000     -> "${"%.0f".format(amount / 1_000)}K"
    else                -> "%.0f".format(amount)
}

private fun categoryColor(category: TransactionCategory): Color = when (category) {
    TransactionCategory.RECEIVE_MONEY -> Color(0xFF00B341)
    TransactionCategory.SEND_MONEY    -> Color(0xFFFF4D4D)
    TransactionCategory.BUY_GOODS     -> Color(0xFF8888FF)
    TransactionCategory.PAY_BILL      -> Color(0xFFFFA040)
    TransactionCategory.AIRTIME       -> Color(0xFFFFD700)
    TransactionCategory.WITHDRAWAL    -> Color(0xFFFF6B6B)
    TransactionCategory.DEPOSIT       -> Color(0xFF00E5A0)
    else                              -> Color(0xFF888888)
}