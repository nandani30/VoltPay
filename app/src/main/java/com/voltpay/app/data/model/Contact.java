package com.voltpay.app.data.model;

import com.google.gson.annotations.SerializedName;

public class Contact {
    @SerializedName("localId")
    private long id;
    private String name;
    private String upiId;
    private String phoneNumber;
    private long createdAt;
    private long lastPaidAt;

    public Contact() {}

    public Contact(String name, String upiId, String phoneNumber, long createdAt) {
        this.name = name;
        this.upiId = upiId;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastPaidAt() { return lastPaidAt; }
    public void setLastPaidAt(long lastPaidAt) { this.lastPaidAt = lastPaidAt; }
}
