package com.voltpay.backend.service;

import com.voltpay.backend.model.PaymentRequest;
import com.voltpay.backend.model.User;
import com.voltpay.backend.repository.PaymentRequestRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentRequestService {

    private final PaymentRequestRepository requestRepository;
    private final UserService userService;
    private final FcmService fcmService;

    public PaymentRequestService(PaymentRequestRepository requestRepository, UserService userService, FcmService fcmService) {
        this.requestRepository = requestRepository;
        this.userService = userService;
        this.fcmService = fcmService;
    }

    public PaymentRequest createRequest(String requesterPhone, String requesterUpiId, String requesterName, String payerPhone, Double amount, String note) {
        PaymentRequest request = new PaymentRequest();
        request.setRequesterPhone(requesterPhone);
        request.setRequesterUpiId(requesterUpiId);
        request.setRequesterName(requesterName);
        request.setPayerPhone(payerPhone);
        request.setAmount(amount);
        request.setNote(note);
        
        PaymentRequest saved = requestRepository.save(request);

        // Send push notification to payer
        userService.getUserByPhone(payerPhone).ifPresent(payer -> {
            java.util.Map<String, String> data = new java.util.HashMap<>();
            data.put("type", "PAYMENT_REQUEST");
            data.put("requestId", saved.getId());
            data.put("requesterName", requesterName);
            data.put("requesterUpiId", requesterUpiId);
            data.put("requesterPhone", requesterPhone);
            data.put("amount", String.valueOf(amount));
            data.put("note", note != null ? note : "");
            
            fcmService.sendDataNotification(payer.getFcmToken(), 
                    "Payment Request from " + requesterName, 
                    requesterName + " is requesting ₹" + amount, data);
        });
        
        return saved;
    }

    public List<PaymentRequest> getIncomingRequests(String phoneNumber) {
        return requestRepository.findByPayerPhoneAndStatus(phoneNumber, "PENDING");
    }

    public List<PaymentRequest> getOutgoingRequests(String phoneNumber) {
        return requestRepository.findByRequesterPhone(phoneNumber);
    }

    public boolean completeRequest(String requestId, String payerPhone) {
        Optional<PaymentRequest> reqOpt = requestRepository.findById(requestId);
        if (reqOpt.isPresent()) {
            PaymentRequest req = reqOpt.get();
            if (req.getPayerPhone().equals(payerPhone) && "PENDING".equals(req.getStatus())) {
                req.setStatus("COMPLETED");
                req.setCompletedAt(Instant.now());
                requestRepository.save(req);
                
                // Notify requester
                userService.getUserByPhone(req.getRequesterPhone()).ifPresent(requester -> {
                    java.util.Map<String, String> data = new java.util.HashMap<>();
                    data.put("type", "REQUEST_PAID");
                    data.put("amount", String.valueOf(req.getAmount()));
                    data.put("name", userService.getUserByPhone(payerPhone).map(User::getName).orElse("Someone"));
                    
                    fcmService.sendDataNotification(requester.getFcmToken(), 
                            "Request Paid", 
                            "Your request for ₹" + req.getAmount() + " was paid.", data);
                });
                return true;
            }
        }
        return false;
    }

    public boolean declineRequest(String requestId, String payerPhone) {
        Optional<PaymentRequest> reqOpt = requestRepository.findById(requestId);
        if (reqOpt.isPresent()) {
            PaymentRequest req = reqOpt.get();
            if (req.getPayerPhone().equals(payerPhone) && "PENDING".equals(req.getStatus())) {
                req.setStatus("DECLINED");
                req.setCompletedAt(Instant.now());
                requestRepository.save(req);
                
                // Notify requester
                userService.getUserByPhone(req.getRequesterPhone()).ifPresent(requester -> {
                    java.util.Map<String, String> data = new java.util.HashMap<>();
                    data.put("type", "REQUEST_DECLINED");
                    data.put("amount", String.valueOf(req.getAmount()));
                    data.put("name", userService.getUserByPhone(payerPhone).map(User::getName).orElse("Someone"));
                    
                    fcmService.sendDataNotification(requester.getFcmToken(), 
                            "Request Declined", 
                            "Your request was declined.", data);
                });
                return true;
            }
        }
        return false;
    }
}
