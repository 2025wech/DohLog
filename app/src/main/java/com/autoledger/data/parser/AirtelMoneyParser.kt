package com.autoledger.data.parser

import com.autoledger.domain.model.MobileMoneyProvider
import com.autoledger.domain.model.Transaction
import com.autoledger.domain.model.TransactionCategory
import com.autoledger.domain.parser.SmsParser
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

class AirtelMoneyParser @Inject constructor() : SmsParser {

    override fun parse(sender: String, messageBody: String): Transaction? {

        val isAirtelSender = sender.contains("airtel", ignoreCase = true) ||
                sender.contains("airtelmoney", ignoreCase = true)

        val isAirtelBody   = messageBody.contains("airtel", ignoreCase = true)

        if (!isAirtelSender && !isAirtelBody) return null

        // Skip non-financial SMS
        val hasAmount = messageBody.contains("Ksh", ignoreCase = true) ||
                messageBody.contains("KES", ignoreCase = true)
        if (!hasAmount) return null

        android.util.Log.d("AIRTEL_PARSER", "PARSING: sender=[$sender]")
        android.util.Log.d("AIRTEL_PARSER", "BODY: $messageBody")

        return try {
            when {
                // ── Received ──────────────────────────────────────────
                // "Received Ksh 20 from NAME 254..."
                messageBody.contains("received", ignoreCase = true) ->
                    parseReceived(messageBody)

                // ── Sent ──────────────────────────────────────────────
                // "Sent Ksh 20 to NAME 254..."
                messageBody.contains("sent", ignoreCase = true) &&
                        messageBody.contains("to", ignoreCase = true) ->
                    parseSent(messageBody)

                // ── Bundle / airtime purchase ─────────────────────────
                // "Bundle purchase successful of Ksh 20"
                // "Airtime purchase of Ksh 20"
                messageBody.contains("bundle", ignoreCase = true) ||
                        messageBody.contains("airtime", ignoreCase = true) ->
                    parseBundleOrAirtime(messageBody)

                // ── Payment ───────────────────────────────────────────
                messageBody.contains("payment", ignoreCase = true) ||
                        messageBody.contains("paid", ignoreCase = true) ->
                    parsePayment(messageBody)

                // ── Withdrawal ────────────────────────────────────────
                messageBody.contains("withdraw", ignoreCase = true) ->
                    parseWithdrawal(messageBody)

                // ── Deposit ───────────────────────────────────────────
                messageBody.contains("deposit", ignoreCase = true) ->
                    parseDeposit(messageBody)

                // ── Any confirmed transaction with balance ────────────
                // "Confirmed. ... Bal:Ksh X"
                messageBody.contains("confirmed", ignoreCase = true) &&
                        extractBalance(messageBody) != null ->
                    parseConfirmed(messageBody)

                else -> {
                    android.util.Log.d("AIRTEL_PARSER",
                        "NO MATCH for body: $messageBody")
                    null
                }
            }?.also {
                android.util.Log.d("AIRTEL_PARSER",
                    "PARSED OK: ${it.category} " +
                            "amount=${it.amount} " +
                            "balance=${it.balance} " +
                            "sender=${it.sender}")
            }
        } catch (e: Exception) {
            android.util.Log.e("AIRTEL_PARSER", "EXCEPTION: ${e.message}")
            null
        }
    }

    // ── Parse received ────────────────────────────────────────────────
    // "TID:O3L7UXUYYDR. Received Ksh 20 from Alex Waweru Mwago
    //  254740595867 on 14/04/26 08:04 PM. Bal:Ksh 31.0"
    private fun parseReceived(body: String): Transaction? {
        val id      = extractTransactionId(body) ?: generateFallbackId(body)
        val amount  = extractAmount(body)         ?: return null
        val balance = extractBalance(body)        ?: 0.0
        val dt      = extractDateTime(body)       ?: LocalDateTime.now()
        val sender  = extractReceivedSender(body)

        return Transaction(
            id, amount, sender, dt, balance,
            TransactionCategory.RECEIVE_MONEY,
            MobileMoneyProvider.AIRTEL_MONEY
        )
    }

    // ── Parse sent ────────────────────────────────────────────────────
    private fun parseSent(body: String): Transaction? {
        val id      = extractTransactionId(body) ?: generateFallbackId(body)
        val amount  = extractAmount(body)         ?: return null
        val balance = extractBalance(body)        ?: 0.0
        val dt      = extractDateTime(body)       ?: LocalDateTime.now()
        val sender  = extractSentRecipient(body)

        return Transaction(
            id, amount, sender, dt, balance,
            TransactionCategory.SEND_MONEY,
            MobileMoneyProvider.AIRTEL_MONEY
        )
    }

    // ── Parse bundle / airtime ────────────────────────────────────────
    // "50712458817 Confirmed. Bundle purchase successful of Ksh 20
    //  via Airtel Networks Kenya Ltd on 14/04/26 at 08:05 PM.
    //  Fee: Ksh 0. Bal: Ksh 11.0."
    private fun parseBundleOrAirtime(body: String): Transaction? {
        val id      = extractTransactionId(body) ?: generateFallbackId(body)
        val amount  = extractAmount(body)         ?: return null
        val balance = extractBalance(body)        ?: 0.0
        val dt      = extractDateTime(body)       ?: LocalDateTime.now()

        val category = if (body.contains("airtime", ignoreCase = true))
            TransactionCategory.AIRTIME
        else
            TransactionCategory.BUY_GOODS

        val sender = if (body.contains("airtime", ignoreCase = true))
            "Airtime" else "Airtel Bundle"

        return Transaction(
            id, amount, sender, dt, balance,
            category,
            MobileMoneyProvider.AIRTEL_MONEY
        )
    }

    // ── Parse payment ─────────────────────────────────────────────────
    private fun parsePayment(body: String): Transaction? {
        val id      = extractTransactionId(body) ?: generateFallbackId(body)
        val amount  = extractAmount(body)         ?: return null
        val balance = extractBalance(body)        ?: 0.0
        val dt      = extractDateTime(body)       ?: LocalDateTime.now()
        val sender  = extractMerchant(body)       ?: "Merchant"

        val category = if (body.contains("account", ignoreCase = true))
            TransactionCategory.PAY_BILL
        else
            TransactionCategory.BUY_GOODS

        return Transaction(
            id, amount, sender, dt, balance,
            category,
            MobileMoneyProvider.AIRTEL_MONEY
        )
    }

    // ── Parse withdrawal ──────────────────────────────────────────────
    private fun parseWithdrawal(body: String): Transaction? {
        val id      = extractTransactionId(body) ?: generateFallbackId(body)
        val amount  = extractAmount(body)         ?: return null
        val balance = extractBalance(body)        ?: 0.0
        val dt      = extractDateTime(body)       ?: LocalDateTime.now()
        val agent   = extractAgent(body)          ?: "Airtel Agent"

        return Transaction(
            id, amount, agent, dt, balance,
            TransactionCategory.WITHDRAWAL,
            MobileMoneyProvider.AIRTEL_MONEY
        )
    }

    // ── Parse deposit ─────────────────────────────────────────────────
    private fun parseDeposit(body: String): Transaction? {
        val id      = extractTransactionId(body) ?: generateFallbackId(body)
        val amount  = extractAmount(body)         ?: return null
        val balance = extractBalance(body)        ?: 0.0
        val dt      = extractDateTime(body)       ?: LocalDateTime.now()
        val agent   = extractAgent(body)          ?: "Airtel Agent"

        return Transaction(
            id, amount, agent, dt, balance,
            TransactionCategory.DEPOSIT,
            MobileMoneyProvider.AIRTEL_MONEY
        )
    }

    // ── Parse any confirmed transaction ───────────────────────────────
    private fun parseConfirmed(body: String): Transaction? {
        val id      = extractTransactionId(body) ?: generateFallbackId(body)
        val amount  = extractAmount(body)         ?: return null
        val balance = extractBalance(body)        ?: 0.0
        val dt      = extractDateTime(body)       ?: LocalDateTime.now()

        return Transaction(
            id, amount, "Airtel Money", dt, balance,
            TransactionCategory.BUY_GOODS,
            MobileMoneyProvider.AIRTEL_MONEY
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // ── Sender extractors ─────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────

    private fun extractReceivedSender(body: String): String {
        // "Received Ksh 20 from Alex Waweru Mwago 254740595867 on"
        Regex(
            """[Rr]eceived\s+Ksh\s*[\d.]+\s+from\s+([A-Za-z][A-Za-z ]+?)\s+(?:254|07|01|\+254)\d+""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.length >= 2 }
            ?.let { return it.titleCase() }

        // "from NAME on date"
        Regex(
            """from\s+([A-Za-z][A-Za-z ]{1,40}?)\s+on\s+\d""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.length >= 2 }
            ?.let { return it.titleCase() }

        // Phone number fallback
        Regex("""(?:254|07|01)\d{8,9}""")
            .find(body)?.value
            ?.let { return formatPhone(it) }

        return "Unknown sender"
    }

    private fun extractSentRecipient(body: String): String {
        // "Sent Ksh 20 to NAME 254..."
        Regex(
            """[Ss]ent\s+Ksh\s*[\d.]+\s+to\s+([A-Za-z][A-Za-z ]+?)\s+(?:254|07|01|\+254)\d+""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.length >= 2 }
            ?.let { return it.titleCase() }

        // "to NAME on"
        Regex(
            """to\s+([A-Za-z][A-Za-z ]{1,40}?)\s+on\s+\d""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.length >= 2 }
            ?.let { return it.titleCase() }

        Regex("""(?:254|07|01)\d{8,9}""")
            .find(body)?.value
            ?.let { return formatPhone(it) }

        return "Unknown recipient"
    }

    private fun extractMerchant(body: String): String? {
        Regex(
            """(?:payment of|paid to)\s+Ksh\s*[\d.]+\s+(?:to\s+)?([A-Za-z][A-Za-z0-9 &]+?)\s+on""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.length >= 2 }
            ?.let { return it.titleCase() }
        return null
    }

    private fun extractAgent(body: String): String? {
        Regex(
            """(?:from|by)\s+([A-Za-z][A-Za-z ]+?)\s+(?:254|07|01|\+254)\d+""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.length >= 2 }
            ?.let { return it.titleCase() }
        return null
    }

    // ─────────────────────────────────────────────────────────────────
    // ── Core extractors — built from REAL SMS formats ─────────────────
    // ─────────────────────────────────────────────────────────────────

    // Real Airtel transaction IDs seen in logs:
    // "TID:O3L7UXUYYDR" and "50712458817"
    private fun extractTransactionId(body: String): String? {
        return listOf(
            // "TID:XXXXXXXXX" format
            Regex("""TID:([A-Z0-9]{8,14})"""),
            // Numeric ID at start like "50712458817 Confirmed"
            Regex("""^(\d{10,14})\s+[Cc]onfirmed"""),
            // Alpha-numeric at start
            Regex("""^([A-Z0-9]{8,14})\s"""),
            // "TID:XXXX" at end "Sender TID:UDEIH0VHNK"
            Regex("""Sender\s+TID:([A-Z0-9]{8,14})""")
        ).firstNotNullOfOrNull { regex ->
            regex.find(body.trim())?.groupValues?.get(1)
        }
    }

    private fun generateFallbackId(body: String): String {
        return "AIR${Math.abs(body.hashCode()).toString().take(8)}"
    }

    // Handles BOTH "Ksh 20" and "KES 20" formats
    // Real Airtel uses "Ksh" — same as M-Pesa!
    private fun extractAmount(body: String): Double? {
        // Skip "Fee: Ksh 0" amounts — look for the main transaction amount
        // Strategy: find first non-zero Ksh/KES amount

        val allAmounts = Regex(
            """(?:Ksh|KES)\.?\s*([\d,]+\.?\d*)""",
            RegexOption.IGNORE_CASE
        ).findAll(body).mapNotNull { match ->
            match.groupValues[1]
                .replace(",", "")
                .toDoubleOrNull()
        }.toList()

        android.util.Log.d("AIRTEL_AMOUNT",
            "All amounts found: $allAmounts")

        // Return first non-zero amount (skip fees of 0)
        return allAmounts.firstOrNull { it > 0 }
    }

    // ── THE KEY FIX — real Airtel balance formats from logs ──────────
    // Format 1: "Bal:Ksh 31.0"       ← seen in received SMS
    // Format 2: "Bal: Ksh 11.0."     ← seen in bundle SMS
    // Format 3: "balance is KES X"   ← standard format
    private fun extractBalance(body: String): Double? {
        android.util.Log.d("AIRTEL_BALANCE", "Extracting from: $body")

        val patterns = listOf(
            // EXACT match from logs: "Bal:Ksh 31.0" or "Bal: Ksh 11.0"
            Regex("""[Bb]al\s*:\s*Ksh\s*([\d,]+\.?\d*)"""),

            // "Bal:KES 31.0"
            Regex("""[Bb]al\s*:\s*KES\s*([\d,]+\.?\d*)""",
                RegexOption.IGNORE_CASE),

            // "Balance: Ksh X" or "Balance:Ksh X"
            Regex("""[Bb]alance\s*:\s*Ksh\s*([\d,]+\.?\d*)"""),

            // "balance is Ksh X" or "balance is KES X"
            Regex("""[Bb]alance\s+is\s+(?:Ksh|KES)\.?\s*([\d,]+\.?\d*)""",
                RegexOption.IGNORE_CASE),

            // "Airtel Money balance Ksh X"
            Regex("""[Aa]irtel\s+[Mm]oney\s+balance\s+(?:is\s+)?(?:Ksh|KES)\.?\s*([\d,]+\.?\d*)""",
                RegexOption.IGNORE_CASE),

            // "New balance Ksh X"
            Regex("""[Nn]ew\s+[Bb]alance\s+(?:Ksh|KES)\.?\s*([\d,]+\.?\d*)""",
                RegexOption.IGNORE_CASE)
        )

        patterns.forEach { regex ->
            val match = regex.find(body)
            if (match != null) {
                val raw    = match.groupValues[1].replace(",", "")
                val parsed = raw.toDoubleOrNull()
                android.util.Log.d("AIRTEL_BALANCE",
                    "MATCHED pattern: ${regex.pattern} → $parsed")
                if (parsed != null) return parsed
            }
        }

        // Nuclear fallback — last Ksh/KES amount in the SMS
        // Balance is always at the END of Airtel SMS
        val allKshAmounts = Regex(
            """(?:Ksh|KES)\.?\s*([\d,]+\.?\d*)""",
            RegexOption.IGNORE_CASE
        ).findAll(body).mapNotNull { match ->
            match.groupValues[1]
                .replace(",", "")
                .toDoubleOrNull()
        }.toList()

        android.util.Log.d("AIRTEL_BALANCE",
            "All Ksh/KES amounts: $allKshAmounts")

        // Return last amount (balance is last in Airtel SMS)
        return allKshAmounts.lastOrNull()
            .also { android.util.Log.d("AIRTEL_BALANCE", "Final: $it") }
    }

    // ── DateTime — real format from logs: "14/04/26 08:04 PM" ────────
    private fun extractDateTime(body: String): LocalDateTime? {

        // Format 1: "14/04/26 08:04 PM" — from real Airtel SMS
        Regex(
            """(\d{1,2}/\d{1,2}/\d{2,4})\s+(\d{1,2}:\d{2}\s*[APap][Mm])"""
        ).find(body)?.let { m ->
            tryBuildDateTime(m.groupValues[1], m.groupValues[2])
                ?.let { return it }
        }

        // Format 2: "on 14/04/26 at 08:05 PM"
        Regex(
            """on\s+(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APap][Mm])""",
            RegexOption.IGNORE_CASE
        ).find(body)?.let { m ->
            tryBuildDateTime(m.groupValues[1], m.groupValues[2])
                ?.let { return it }
        }

        // Format 3: "03/04/2026 14:30:00" — 24hr
        Regex("""(\d{2}/\d{2}/\d{4})\s+(\d{2}:\d{2}:\d{2})""")
            .find(body)?.let { m ->
                try {
                    return LocalDateTime.parse(
                        "${m.groupValues[1]} ${m.groupValues[2]}",
                        DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy HH:mm:ss", Locale.ROOT
                        )
                    )
                } catch (_: Exception) {}
            }

        return LocalDateTime.now()
    }

    private fun tryBuildDateTime(
        datePart : String,
        timePart : String
    ): LocalDateTime? {
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ROOT)
        val dateFormats   = listOf(
            "d/M/yy", "dd/MM/yy",
            "d/M/yyyy", "dd/MM/yyyy"
        )
        for (fmt in dateFormats) {
            try {
                val date = LocalDate.parse(
                    datePart,
                    DateTimeFormatter.ofPattern(fmt, Locale.ROOT)
                )
                val time = LocalTime.parse(
                    timePart.trim().uppercase(Locale.ROOT),
                    timeFormatter
                )
                return LocalDateTime.of(date, time)
            } catch (_: Exception) {}
        }
        return null
    }

    private fun formatPhone(phone: String): String {
        val normalized = when {
            phone.startsWith("+254") -> "0" + phone.substring(4)
            phone.startsWith("254")  -> "0" + phone.substring(3)
            else                     -> phone
        }
        return if (normalized.length == 10) {
            "${normalized.substring(0, 4)} " +
                    "${normalized.substring(4, 7)} " +
                    normalized.substring(7)
        } else normalized
    }

    private fun String.titleCase(): String =
        lowercase(Locale.ROOT).split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase(Locale.ROOT) }
        }
}