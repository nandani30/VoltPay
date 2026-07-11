package com.voltpay.app.data.model

data class Contact(
    var id: Long = 0,
    var name: String? = null,
    var upiId: String? = null,
    var phoneNumber: String? = null,
    var createdAt: Long = 0,
    var lastPaidAt: Long = 0
)
