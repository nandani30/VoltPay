package com.voltpay.backend.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "users")
data class User(
    @Id var id: String? = null,
    var phoneNumber: String? = null,
    var name: String? = null,
    var upiId: String? = null,
    var fcmToken: String? = null,
    var registeredAt: Instant = Instant.now(),
    var lastSeen: Instant = Instant.now(),
    var authToken: String? = null,
    var tokenExpiry: Instant? = null
)
