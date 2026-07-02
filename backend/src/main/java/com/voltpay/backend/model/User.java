package com.voltpay.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String phoneNumber;
    private String name;
    private String upiId;
    private String fcmToken;
    private Instant registeredAt = Instant.now();
    private Instant lastSeen = Instant.now();
    
    // Auth fields
    private String authToken;
    private Instant tokenExpiry;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
    
    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    
    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
    
    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }

    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    public Instant getTokenExpiry() { return tokenExpiry; }
    public void setTokenExpiry(Instant tokenExpiry) { this.tokenExpiry = tokenExpiry; }
}
