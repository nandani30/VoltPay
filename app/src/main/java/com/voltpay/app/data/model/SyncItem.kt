package com.voltpay.app.data.model

data class SyncItem(
    var id: Long = 0,
    var actionType: String? = null,
    var payload: String? = null,
    var createdAt: Long = 0,
    var retryCount: Int = 0,
    var lastAttempted: Long = 0,
    var status: String? = null // PENDING, PROCESSING, FAILED
)
