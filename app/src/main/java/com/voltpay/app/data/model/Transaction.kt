package com.voltpay.app.data.model

data class Transaction(
    var id: Long = 0,
    var amount: Double = 0.0,
    var type: String? = null, // DEBIT or CREDIT
    var upiRef: String? = null,
    var counterparty: String? = null,
    var timestamp: Long = 0,
    var rawSms: String? = null,
    var source: String? = null // SMS or MANUAL
)
