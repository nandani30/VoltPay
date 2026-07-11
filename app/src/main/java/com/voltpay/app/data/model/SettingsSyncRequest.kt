package com.voltpay.app.data.model

data class SettingsSyncRequest(
    var displayName: String? = null,
    var ownUpiId: String? = null,
    var simSlotIndex: Int? = null
)
