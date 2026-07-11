package com.voltpay.backend.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "settings_backup")
data class SettingsBackup(
    @Id var id: String? = null,
    var phoneNumber: String? = null,
    var displayName: String? = null,
    var ownUpiId: String? = null,
    var simSlotIndex: Int? = null,
    var updatedAt: Instant = Instant.now()
)
