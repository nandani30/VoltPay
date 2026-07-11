package com.voltpay.app.data.model

data class ContactSyncResponse(
    var success: Boolean = false,
    var syncedAt: Long = 0,
    var contacts: List<Contact>? = null
)
