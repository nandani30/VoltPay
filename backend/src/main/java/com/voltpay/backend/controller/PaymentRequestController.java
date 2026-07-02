package com.voltpay.backend.controller;

import com.voltpay.backend.model.PaymentRequest;
import com.voltpay.backend.service.PaymentRequestService;
import com.voltpay.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/requests")
public class PaymentRequestController {

    private final PaymentRequestService requestService;
    private final UserService userService;
    
    // Basic rate limiter: max 10 requests per minute per phone number
    private final Map<String, List<Long>> rateLimiter = new ConcurrentHashMap<>();

    public PaymentRequestController(PaymentRequestService requestService, UserService userService) {
        this.requestService = requestService;
        this.userService = userService;
    }

    private boolean isRateLimited(String phoneNumber) {
        long now = System.currentTimeMillis();
        List<Long> timestamps = rateLimiter.computeIfAbsent(phoneNumber, k -> new ArrayList<>());
        
        synchronized (timestamps) {
            timestamps.removeIf(t -> now - t > 60000);
            if (timestamps.size() >= 10) {
                return true;
            }
            timestamps.add(now);
        }
        return false;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createRequest(@RequestHeader("Authorization") String token, 
                                           @RequestBody CreateRequestDto dto) {
        if (!userService.validateToken(dto.getRequesterPhone(), token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        
        if (isRateLimited(dto.getRequesterPhone())) {
            return ResponseEntity.status(429).body(Map.of("error", "Too Many Requests. Please wait."));
        }

        PaymentRequest request = requestService.createRequest(
                dto.getRequesterPhone(), dto.getRequesterUpiId(), 
                dto.getRequesterName(), dto.getPayerPhone(), 
                dto.getAmount(), dto.getNote());
        return ResponseEntity.ok(Map.of("requestId", request.getId(), "success", true));
    }

    @GetMapping("/incoming/{phoneNumber}")
    public ResponseEntity<?> getIncoming(@RequestHeader("Authorization") String token, 
                                         @PathVariable String phoneNumber) {
        if (!userService.validateToken(phoneNumber, token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        return ResponseEntity.ok(requestService.getIncomingRequests(phoneNumber));
    }

    @GetMapping("/outgoing/{phoneNumber}")
    public ResponseEntity<?> getOutgoing(@RequestHeader("Authorization") String token, 
                                         @PathVariable String phoneNumber) {
        if (!userService.validateToken(phoneNumber, token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        return ResponseEntity.ok(requestService.getOutgoingRequests(phoneNumber));
    }

    @PutMapping("/{requestId}/complete")
    public ResponseEntity<?> completeRequest(@RequestHeader("Authorization") String token, 
                                             @PathVariable String requestId, 
                                             @RequestBody CompleteRequestDto dto) {
        if (!userService.validateToken(dto.getPayerPhone(), token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        boolean success = requestService.completeRequest(requestId, dto.getPayerPhone());
        return ResponseEntity.ok(Map.of("success", success));
    }

    @PutMapping("/{requestId}/decline")
    public ResponseEntity<?> declineRequest(@RequestHeader("Authorization") String token, 
                                            @PathVariable String requestId, 
                                            @RequestBody CompleteRequestDto dto) {
        if (!userService.validateToken(dto.getPayerPhone(), token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        boolean success = requestService.declineRequest(requestId, dto.getPayerPhone());
        return ResponseEntity.ok(Map.of("success", success));
    }

    static class CreateRequestDto {
        private String requesterPhone;
        private String requesterUpiId;
        private String requesterName;
        private String payerPhone;
        private Double amount;
        private String note;
        
        public String getRequesterPhone() { return requesterPhone; }
        public void setRequesterPhone(String requesterPhone) { this.requesterPhone = requesterPhone; }
        
        public String getRequesterUpiId() { return requesterUpiId; }
        public void setRequesterUpiId(String requesterUpiId) { this.requesterUpiId = requesterUpiId; }
        
        public String getRequesterName() { return requesterName; }
        public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
        
        public String getPayerPhone() { return payerPhone; }
        public void setPayerPhone(String payerPhone) { this.payerPhone = payerPhone; }
        
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    static class CompleteRequestDto {
        private String payerPhone;
        
        public String getPayerPhone() { return payerPhone; }
        public void setPayerPhone(String payerPhone) { this.payerPhone = payerPhone; }
    }
}
