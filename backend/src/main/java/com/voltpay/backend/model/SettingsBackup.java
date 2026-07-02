package com.voltpay.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "settings_backup")
public class SettingsBackup {
    @Id
    private String id;
    private String phoneNumber;
    private String displayName;
    private String ownUpiId;
    private Integer simSlotIndex;
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getOwnUpiId() { return ownUpiId; }
    public void setOwnUpiId(String ownUpiId) { this.ownUpiId = ownUpiId; }

    public Integer getSimSlotIndex() { return simSlotIndex; }
    public void setSimSlotIndex(Integer simSlotIndex) { this.simSlotIndex = simSlotIndex; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
