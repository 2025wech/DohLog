package com.autoledger.data.receiver

import android.content.Context
import android.content.SharedPreferences
import android.provider.Telephony
import com.autoledger.data.parser.AirtelMoneyParser
import com.autoledger.data.parser.MpesaParser
import com.autoledger.domain.model.Transaction
import com.autoledger.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository   : TransactionRepository,
    private val mpesaParser  : MpesaParser,
    private val airtelParser : AirtelMoneyParser
) {
    companion object {
        // Initial import window — last 24 hours on first launch
        private const val IMPORT_WINDOW_MS  = 24L * 60 * 60 * 1000

        // SharedPreferences keys
        private const val PREFS_NAME        = "autoledger_importer"
        private const val KEY_LAST_IMPORT   = "last_import_timestamp"
        private const val KEY_FIRST_IMPORT  = "first_import_done"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun importAll() = withContext(Dispatchers.IO) {
        val transactions  = mutableListOf<Transaction>()
        val now           = System.currentTimeMillis()
        val isFirstImport = !prefs.getBoolean(KEY_FIRST_IMPORT, false)

        // On first launch  → import last 24 hrs as starting point
        // On subsequent    → import from last successful import timestamp
        //                    so NO message is ever missed between sessions
        val cutoffTime = if (isFirstImport) {
            now - IMPORT_WINDOW_MS          // 24 hrs ago
        } else {
            // Pick up exactly where we left off last time
            prefs.getLong(KEY_LAST_IMPORT, now - IMPORT_WINDOW_MS)
        }

        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            ),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(cutoffTime.toString()),
            "${Telephony.Sms.DATE} ASC"     // oldest first so Room inserts in order
        )

        cursor?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIndex    = it.getColumnIndex(Telephony.Sms.BODY)

            while (it.moveToNext()) {
                val sender = it.getString(addressIndex) ?: continue
                val body   = it.getString(bodyIndex)    ?: continue
                // Add this temporarily inside the cursor?.use { } block
                // right before the parser lines
                android.util.Log.d("SMS_RAW", "=== SMS ===")
                android.util.Log.d("SMS_RAW", "SENDER: [$sender]")
                android.util.Log.d("SMS_RAW", "BODY: $body")

                val transaction = mpesaParser.parse(sender, body)
                    ?: airtelParser.parse(sender, body)
                    ?: continue

                transactions.add(transaction)
            }
        }

        if (transactions.isNotEmpty()) {
            repository.saveAll(transactions)
        }

        // ── Save state so next import continues from here ─────────────
        prefs.edit()
            .putBoolean(KEY_FIRST_IMPORT, true)
            .putLong(KEY_LAST_IMPORT, now)      // next time starts from now
            .apply()

        transactions.size
    }
}