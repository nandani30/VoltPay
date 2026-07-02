package com.voltpay.backend.repository;

import com.voltpay.backend.model.PaymentRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PaymentRequestRepository extends MongoRepository<PaymentRequest, String> {
    List<PaymentRequest> findByPayerPhoneAndStatus(String payerPhone, String status);
    List<PaymentRequest> findByRequesterPhone(String requesterPhone);
}
