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

class MpesaParser @Inject constructor() : SmsParser {

    override fun parse(sender: String, messageBody: String): Transaction? {
        if (!sender.equals("MPESA", ignoreCase = true) &&
            !sender.contains("MPESA", ignoreCase = true)
        ) return null

        return try {
            when {
                // ── Received money ────────────────────────────────────
                // "You have received Ksh45.00 from..."
                // "received Ksh45.00 from..."
                messageBody.contains("received", ignoreCase = true) &&
                        messageBody.contains("from", ignoreCase = true) ->
                    parseReceived(messageBody)

                // ── Sent money ────────────────────────────────────────
                // "Ksh500.00 sent to JOHN DOE 0712345678 on..."
                messageBody.contains("sent to", ignoreCase = true) ->
                    parseSent(messageBody)

                // ── Buy goods (Till number) ───────────────────────────
                // "Ksh500.00 paid to JAVA HOUSE on..."
                messageBody.contains("paid to", ignoreCase = true) &&
                        !messageBody.contains("account number", ignoreCase = true) ->
                    parsePaidTo(messageBody, TransactionCategory.BUY_GOODS)

                // ── Pay bill ──────────────────────────────────────────
                // "Ksh2,000.00 paid to KPLC PREPAID Account Number..."
                messageBody.contains("paid to", ignoreCase = true) &&
                        messageBody.contains("account number", ignoreCase = true) ->
                    parsePaidTo(messageBody, TransactionCategory.PAY_BILL)

                // ── Withdraw ──────────────────────────────────────────
                // "Withdraw Ksh2,000.00 from 254712345678 - JOHN AGENT"
                messageBody.contains("Withdraw", ignoreCase = true) ->
                    parseWithdrawal(messageBody)

                // ── Airtime ───────────────────────────────────────────
                // "Ksh50.00 of airtime for 0712345678 on..."
                messageBody.contains("airtime", ignoreCase = true) ->
                    parseAirtime(messageBody)

                // ── Deposit ───────────────────────────────────────────
                // "Ksh1,000.00 deposited to your M-PESA account by..."
                messageBody.contains("deposited", ignoreCase = true) ->
                    parseDeposit(messageBody)

                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ── Parse: received money ─────────────────────────────────────────
    private fun parseReceived(body: String): Transaction? {
        val id      = extractTransactionId(body) ?: return null
        val amount  = extractAmount(body)         ?: return null
        val balance = extractBalance(body)        ?: return null
        val dt      = extractDateTime(body)       ?: return null
        val sender  = extractReceivedSender(body)

        return Transaction(
            id, amount, sender, dt, balance,
            TransactionCategory.RECEIVE_MONEY,
            MobileMoneyProvider.MPESA
        )
    }

    // ── Parse: sent money ─────────────────────────────────────────────
    private fun parseSent(body: String): Transaction? {
        val id      = extractTransactionId(body) ?: return null
        val amount  = extractAmount(body)         ?: return null
        val balance = extractBalance(body)        ?: return null
        val dt      = extractDateTime(body)       ?: return null
        val sender  = extractSentRecipient(body)

        return Transaction(
            id, amount, sender, dt, balance,
            TransactionCategory.SEND_MONEY,
            MobileMoneyProvider.MPESA
        )
    }

    // ── Parse: paid to (buy goods or pay bill) ────────────────────────
    private fun parsePaidTo(
        body     : String,
        category : TransactionCategory
    ): Transaction? {
        val id      = extractTransactionId(body) ?: return null
        val amount  = extractAmount(body)         ?: return null
        val balance = extractBalance(body)        ?: return null
        val dt      = extractDateTime(body)       ?: return null
        val sender  = extractMerchantName(body)   ?: "Merchant"

        return Transaction(
            id, amount, sender, dt, balance,
            category,
            MobileMoneyProvider.MPESA
        )
    }

    // ── Parse: withdrawal ─────────────────────────────────────────────
    private fun parseWithdrawal(body: String): Transaction? {
        val id      = extractTransactionId(body) ?: return null
        val amount  = extractAmount(body)         ?: return null
        val balance = extractBalance(body)        ?: return null
        val dt      = extractDateTime(body)       ?: return null

        // "from 254712345678 - JOHN AGENT New"
        val agentRegex = Regex(
            """from\s+\d+\s*-\s*([A-Za-z ]+?)(?:\s+New|\s+M-PESA|\.)""",
            RegexOption.IGNORE_CASE
        )
        val sender = agentRegex.find(body)
            ?.groupValues?.get(1)?.trim()
            ?.titleCase()
            ?: "Agent"

        return Transaction(
            id, amount, sender, dt, balance,
            TransactionCategory.WITHDRAWAL,
            MobileMoneyProvider.MPESA
        )
    }

    // ── Parse: airtime ────────────────────────────────────────────────
    private fun parseAirtime(body: String): Transaction? {
        val id      = extractTransactionId(body) ?: return null
        val amount  = extractAmount(body)         ?: return null
        val balance = extractBalance(body)        ?: return null
        val dt      = extractDateTime(body)       ?: return null

        return Transaction(
            id, amount, "Airtime", dt, balance,
            TransactionCategory.AIRTIME,
            MobileMoneyProvider.MPESA
        )
    }

    // ── Parse: deposit ────────────────────────────────────────────────
    private fun parseDeposit(body: String): Transaction? {
        val id      = extractTransactionId(body) ?: return null
        val amount  = extractAmount(body)         ?: return null
        val balance = extractBalance(body)        ?: return null
        val dt      = extractDateTime(body)       ?: return null

        // "deposited to your M-PESA account by JOHN AGENT 254712345678 on"
        val byRegex = Regex(
            """by\s+([A-Za-z][A-Za-z ]{1,40}?)\s+(?:\+?254|07|01)\d+""",
            RegexOption.IGNORE_CASE
        )
        val sender = byRegex.find(body)
            ?.groupValues?.get(1)?.trim()
            ?.titleCase()
            ?: extractNameAfterKeyword(body, "by")
            ?: "Agent"

        return Transaction(
            id, amount, sender, dt, balance,
            TransactionCategory.DEPOSIT,
            MobileMoneyProvider.MPESA
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // ── Sender extractors ─────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────

    private fun extractReceivedSender(body: String): String {

        // ── Pattern 1: name + phone number ───────────────────────────
        // "received Ksh45.00 from JOHN DOE 0712345678 on"
        Regex(
            """received\s+Ksh[\d,.]+\s+from\s+([A-Z][A-Z ]+?)\s+(?:\+?254|07|01)\d+""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.length >= 2 && !it.equals("on", ignoreCase = true) }
            ?.let { return it.titleCase() }

        // ── Pattern 2: phone number only — no name ───────────────────
        // "received Ksh45.00 from 0712345678 on"
        Regex(
            """received\s+Ksh[\d,.]+\s+from\s+((?:\+?254|07|01)\d{8,9})\s+on""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
            ?.let { return formatPhone(it) }

        // ── Pattern 3: name before "on" keyword ──────────────────────
        // "received Ksh45.00 from SAFARICOM on 10/4/26"
        Regex(
            """received\s+Ksh[\d,.]+\s+from\s+([A-Za-z][A-Za-z0-9 ]{1,40}?)\s+on\s+\d""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.length >= 2 }
            ?.let { return it.titleCase() }

        // ── Pattern 4: generic "from X on date" ──────────────────────
        Regex(
            """from\s+(.+?)\s+on\s+\d{1,2}/\d{1,2}""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotEmpty() && it.length >= 2 }
            ?.let { raw ->
                // Strip trailing phone number if present
                val cleaned = raw
                    .replace(Regex("""\s+(?:\+?254|07|01)\d{8,9}$"""), "")
                    .trim()
                if (cleaned.length >= 2) return cleaned.titleCase()
            }

        // ── Pattern 5: any phone number in the SMS ────────────────────
        Regex("""(?:\+?254|07|01)\d{8,9}""")
            .find(body)?.value
            ?.let { return formatPhone(it) }

        return "Unknown sender"
    }

    private fun extractSentRecipient(body: String): String {

        // ── Pattern 1: name + phone number ───────────────────────────
        // "sent to JOHN DOE 0712345678 on"
        Regex(
            """sent\s+to\s+([A-Z][A-Z ]+?)\s+(?:\+?254|07|01)\d+""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.length >= 2 && !it.equals("on", ignoreCase = true) }
            ?.let { return it.titleCase() }

        // ── Pattern 2: phone number only ─────────────────────────────
        // "sent to 0712345678 on"
        Regex(
            """sent\s+to\s+((?:\+?254|07|01)\d{8,9})\s+on""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
            ?.let { return formatPhone(it) }

        // ── Pattern 3: name before "on" ──────────────────────────────
        // "sent to MERCHANT NAME on 10/4/26"
        Regex(
            """sent\s+to\s+([A-Za-z][A-Za-z ]{1,40}?)\s+on\s+\d""",
            RegexOption.IGNORE_CASE
        ).find(body)?.groupValues?.get(1)?.trim()
            ?.takeIf { it.length >= 2 }
            ?.let { return it.titleCase() }

        // ── Pattern 4: any phone in SMS ──────────────────────────────
        Regex("""(?:\+?254|07|01)\d{8,9}""")
            .find(body)?.value
            ?.let { return formatPhone(it) }

        return "Unknown recipient"
    }

    private fun extractMerchantName(body: String): String? {
        // "paid to JAVA HOUSE on" or "paid to KPLC PREPAID Account"
        listOf(
            Regex(
                """paid\s+to\s+([A-Z0-9&. ]+?)\s+Account""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """paid\s+to\s+([A-Z0-9&. ]+?)\s+on\s+\d""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """paid\s+to\s+([A-Z0-9&. ]+?)\s+New\s""",
                RegexOption.IGNORE_CASE
            )
        ).forEach { regex ->
            regex.find(body)?.groupValues?.get(1)?.trim()
                ?.takeIf { it.length >= 2 }
                ?.let { return it.titleCase() }
        }
        return null
    }

    private fun extractNameAfterKeyword(
        body    : String,
        keyword : String
    ): String? {
        listOf(
            // Name then phone number
            Regex(
                """${Regex.escape(keyword)}\s+([A-Za-z][A-Za-z ]{1,40}?)\s+(?:\+?254|07|01)\d+""",
                RegexOption.IGNORE_CASE
            ),
            // Name then "on" + date
            Regex(
                """${Regex.escape(keyword)}\s+([A-Za-z][A-Za-z ]{1,40}?)\s+on\s+\d""",
                RegexOption.IGNORE_CASE
            ),
            // Name then "New"
            Regex(
                """${Regex.escape(keyword)}\s+([A-Za-z][A-Za-z ]{1,40}?)\s+New\s""",
                RegexOption.IGNORE_CASE
            )
        ).forEach { pattern ->
            pattern.find(body)?.groupValues?.get(1)?.trim()
                ?.takeIf { it.length >= 2 &&
                        !it.equals("your", ignoreCase = true) &&
                        !it.equals("the", ignoreCase = true) }
                ?.let { return it.titleCase() }
        }
        return null
    }

    // ─────────────────────────────────────────────────────────────────
    // ── Core extractors ───────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────

    // Real M-Pesa IDs: "QHX2K8J3PL" — 10 alphanumeric at start of SMS
    private fun extractTransactionId(body: String): String? {
        return Regex("""^([A-Z0-9]{10})\s""")
            .find(body.trim())?.groupValues?.get(1)
            ?: Regex("""([A-Z0-9]{10})\s+Confirmed""")
                .find(body)?.groupValues?.get(1)
    }

    // Handles: "Ksh1,500.00" "Ksh500" "KSh1500.00" "Ksh.500"
    private fun extractAmount(body: String): Double? {
        return Regex("""[Kk][Ss][Hh]\.?\s*([\d,]+\.?\d*)""")
            .find(body)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
    }

    // "New M-PESA balance is Ksh8,250.00"
    // "M-PESA balance is Ksh0.00"
    private fun extractBalance(body: String): Double? {
        return Regex(
            """balance\s+is\s+[Kk][Ss][Hh]\.?\s*([\d,]+\.?\d*)"""
        ).find(body)?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
    }

    // ─────────────────────────────────────────────────────────────────
    // ── DateTime extractor ────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────

    private fun extractDateTime(body: String): LocalDateTime? {

        // ── Format 1: "10/4/26 at 7:23 PM" ─── most common ──────────
        Regex(
            """(\d{1,2}/\d{1,2}/\d{2,4})\s+at\s+(\d{1,2}:\d{2}\s*[APap][Mm])"""
        ).find(body)?.let { m ->
            val datePart = m.groupValues[1]
            val timePart = m.groupValues[2].trim().uppercase(Locale.ROOT)

            // Try short year first: d/M/yy
            tryBuildDateTime(datePart, timePart, "d/M/yy")
                ?.let { return it }

            // Try full year: d/M/yyyy
            tryBuildDateTime(datePart, timePart, "d/M/yyyy")
                ?.let { return it }

            // Try zero-padded: dd/MM/yyyy
            tryBuildDateTime(datePart, timePart, "dd/MM/yyyy")
                ?.let { return it }
        }

        // ── Format 2: "03/04/2026 14:30:00" ──────────────────────────
        Regex("""(\d{2}/\d{2}/\d{4}\s+\d{2}:\d{2}:\d{2})""")
            .find(body)?.let { m ->
                try {
                    return LocalDateTime.parse(
                        m.groupValues[1],
                        DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy HH:mm:ss", Locale.ROOT
                        )
                    )
                } catch (_: Exception) {}
            }

        // ── Format 3: "03/04/2026 14:30" ─────────────────────────────
        Regex("""(\d{2}/\d{2}/\d{4}\s+\d{2}:\d{2})""")
            .find(body)?.let { m ->
                try {
                    return LocalDateTime.parse(
                        m.groupValues[1],
                        DateTimeFormatter.ofPattern(
                            "dd/MM/yyyy HH:mm", Locale.ROOT
                        )
                    )
                } catch (_: Exception) {}
            }

        // ── Fallback: use current time so transaction still saves ─────
        return LocalDateTime.now()
    }

    private fun tryBuildDateTime(
        datePart   : String,
        timePart   : String,
        datePattern: String
    ): LocalDateTime? {
        return try {
            val date = LocalDate.parse(
                datePart,
                DateTimeFormatter.ofPattern(datePattern, Locale.ROOT)
            )
            val time = LocalTime.parse(
                timePart,
                DateTimeFormatter.ofPattern("h:mm a", Locale.ROOT)
            )
            LocalDateTime.of(date, time)
        } catch (_: Exception) {
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ── Utility helpers ───────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────

    // Normalizes any Kenyan phone format to "07XX XXX XXX"
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

    // "JOHN DOE" → "John Doe" / "SAFARICOM" → "Safaricom"
    private fun String.titleCase(): String =
        lowercase(Locale.ROOT)
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase(Locale.ROOT) }
            }
}