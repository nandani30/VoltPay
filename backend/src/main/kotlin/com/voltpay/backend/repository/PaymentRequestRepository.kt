package com.voltpay.backend.repository

import com.voltpay.backend.model.PaymentRequest
import org.springframework.data.mongodb.repository.MongoRepository

interface PaymentRequestRepository : MongoRepository<PaymentRequest, String> {
    fun findByPayerPhoneAndStatus(payerPhone: String, status: String): List<PaymentRequest>
    fun findByRequesterPhone(requesterPhone: String): List<PaymentRequest>
}
