package com.voltpay.backend.controller

import com.voltpay.backend.service.PaymentRequestService
import com.voltpay.backend.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/requests")
class PaymentRequestController(
    private val requestService: PaymentRequestService,
    private val userService: UserService
) {

    @PostMapping("/create")
    fun createRequest(
        @RequestHeader("Authorization") token: String,
        @RequestBody dto: CreateRequestDto
    ): ResponseEntity<*> {
        if (!userService.validateToken(dto.requesterPhone ?: "", token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        }
        val req = requestService.createRequest(
            dto.requesterPhone ?: "",
            dto.requesterUpiId,
            dto.payerPhone ?: "",
            dto.amount ?: 0.0,
            dto.note
        )
        return ResponseEntity.ok(mapOf("success" to true, "requestId" to req.id))
    }

    @GetMapping("/pending/{payerPhone}")
    fun getPendingRequests(
        @RequestHeader("Authorization") token: String,
        @PathVariable payerPhone: String
    ): ResponseEntity<*> {
        if (!userService.validateToken(payerPhone, token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        }
        return ResponseEntity.ok(requestService.getPendingRequestsForPayer(payerPhone))
    }

    @PutMapping("/{requestId}/complete")
    fun completeRequest(
        @RequestHeader("Authorization") token: String,
        @PathVariable requestId: String,
        @RequestBody dto: CompleteRequestDto
    ): ResponseEntity<*> {
        if (!userService.validateToken(dto.payerPhone ?: "", token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        }
        val success = requestService.completeRequest(requestId, dto.payerPhone ?: "")
        return if (success) ResponseEntity.ok(mapOf("success" to true))
        else ResponseEntity.badRequest().body(mapOf("error" to "Could not complete request"))
    }

    @PutMapping("/{requestId}/decline")
    fun declineRequest(
        @RequestHeader("Authorization") token: String,
        @PathVariable requestId: String,
        @RequestBody dto: CompleteRequestDto
    ): ResponseEntity<*> {
        if (!userService.validateToken(dto.payerPhone ?: "", token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        }
        val success = requestService.declineRequest(requestId, dto.payerPhone ?: "")
        return if (success) ResponseEntity.ok(mapOf("success" to true))
        else ResponseEntity.badRequest().body(mapOf("error" to "Could not decline request"))
    }

    data class CreateRequestDto(
        var requesterPhone: String? = null,
        var requesterUpiId: String? = null,
        var requesterName: String? = null,
        var payerPhone: String? = null,
        var amount: Double? = null,
        var note: String? = null
    )

    data class CompleteRequestDto(
        var payerPhone: String? = null
    )
}
