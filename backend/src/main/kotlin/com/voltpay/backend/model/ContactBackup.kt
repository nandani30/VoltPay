package com.voltpay.backend.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "contacts_backup")
data class ContactBackup(
    @Id var id: String? = null,
    var phoneNumber: String? = null,
    var contacts: List<ContactEntry>? = null,
    var lastSyncedAt: Instant = Instant.now()
) {
    data class ContactEntry(
        var localId: Long? = null,
        var name: String? = null,
        var upiId: String? = null,
        var phoneNumber: String? = null,
        var createdAt: Instant? = null,
        var lastPaidAt: Instant? = null
    )
}
