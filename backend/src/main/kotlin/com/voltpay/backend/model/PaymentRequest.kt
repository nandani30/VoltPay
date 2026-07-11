package com.voltpay.backend.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "payment_requests")
data class PaymentRequest(
    @Id var id: String? = null,
    var requesterPhone: String? = null,
    var requesterUpiId: String? = null,
    var requesterName: String? = null,
    var payerPhone: String? = null,
    var amount: Double? = null,
    var note: String? = null,
    var status: String = "PENDING", // PENDING, COMPLETED, CANCELLED
    var createdAt: Instant = Instant.now(),
    var completedAt: Instant? = null
)
