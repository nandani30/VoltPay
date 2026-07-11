package com.voltpay.backend.service

import com.voltpay.backend.model.PaymentRequest
import com.voltpay.backend.repository.PaymentRequestRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PaymentRequestService(
    private val requestRepository: PaymentRequestRepository,
    private val userService: UserService
) {

    fun createRequest(requesterPhone: String, requesterUpiId: String?, payerPhone: String, amount: Double, note: String?): PaymentRequest {
        val req = PaymentRequest(
            requesterPhone = requesterPhone,
            requesterUpiId = requesterUpiId,
            payerPhone = payerPhone,
            amount = amount,
            note = note
        )
        // Set requester name if user exists
        userService.getUserByPhone(requesterPhone).ifPresent { req.requesterName = it.name }
        return requestRepository.save(req)
    }

    fun getPendingRequestsForPayer(phoneNumber: String): List<PaymentRequest> {
        return requestRepository.findByPayerPhoneAndStatus(phoneNumber, "PENDING")
    }

    fun getRequestsByRequester(phoneNumber: String): List<PaymentRequest> {
        return requestRepository.findByRequesterPhone(phoneNumber)
    }

    fun completeRequest(requestId: String, payerPhone: String): Boolean {
        val reqOpt = requestRepository.findById(requestId)
        if (reqOpt.isPresent) {
            val req = reqOpt.get()
            if (req.payerPhone == payerPhone && req.status == "PENDING") {
                req.status = "COMPLETED"
                req.completedAt = Instant.now()
                requestRepository.save(req)
                return true
            }
        }
        return false
    }

    fun declineRequest(requestId: String, payerPhone: String): Boolean {
        val reqOpt = requestRepository.findById(requestId)
        if (reqOpt.isPresent) {
            val req = reqOpt.get()
            if (req.payerPhone == payerPhone && req.status == "PENDING") {
                req.status = "DECLINED"
                req.completedAt = Instant.now()
                requestRepository.save(req)
                return true
            }
        }
        return false
    }
}
