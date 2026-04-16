package com.autoledger.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Telephony
import com.autoledger.data.parser.AirtelMoneyParser
import com.autoledger.data.parser.MpesaParser
import com.autoledger.domain.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var repository  : TransactionRepository
    @Inject lateinit var mpesaParser : MpesaParser
    @Inject lateinit var airtelParser: AirtelMoneyParser

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Update last seen timestamp so importer never re-imports
        // messages that SmsReceiver already handled
        val prefs: SharedPreferences = context.getSharedPreferences(
            "autoledger_importer", Context.MODE_PRIVATE
        )

        messages.forEach { smsMessage ->
            val sender = smsMessage.originatingAddress ?: return@forEach
            val body   = smsMessage.messageBody        ?: return@forEach

            val transaction = mpesaParser.parse(sender, body)
                ?: airtelParser.parse(sender, body)
                ?: return@forEach

            CoroutineScope(Dispatchers.IO).launch {
                repository.saveTransaction(transaction)

                // Advance the timestamp so next importAll() starts after this
                prefs.edit()
                    .putLong(
                        "last_import_timestamp",
                        System.currentTimeMillis()
                    )
                    .apply()
            }
        }
    }
}