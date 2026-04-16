package com.autoledger.domain.parser

import com.autoledger.domain.model.Transaction

interface SmsParser {
    // Returns a Transaction if the SMS matches this provider,
    // null if it doesn't belong to this parser
    fun parse(sender: String, messageBody: String): Transaction?
}