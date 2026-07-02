package com.voltpay.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "payment_requests")
public class PaymentRequest {
    @Id
    private String id;
    private String requesterPhone;
    private String requesterUpiId;
    private String requesterName;
    private String payerPhone;
    private Double amount;
    private String note;
    private String status = "PENDING"; // PENDING, COMPLETED, CANCELLED
    private Instant createdAt = Instant.now();
    private Instant completedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
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
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
