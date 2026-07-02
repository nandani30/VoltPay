package com.voltpay.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Document(collection = "contacts_backup")
public class ContactBackup {
    @Id
    private String id;
    private String phoneNumber;
    private List<ContactEntry> contacts;
    private Instant lastSyncedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public List<ContactEntry> getContacts() { return contacts; }
    public void setContacts(List<ContactEntry> contacts) { this.contacts = contacts; }

    public Instant getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public static class ContactEntry {
        private Long localId;
        private String name;
        private String upiId;
        private String phoneNumber;
        private Instant createdAt;
        private Instant lastPaidAt;

        public Long getLocalId() { return localId; }
        public void setLocalId(Long localId) { this.localId = localId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUpiId() { return upiId; }
        public void setUpiId(String upiId) { this.upiId = upiId; }

        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

        public Instant getLastPaidAt() { return lastPaidAt; }
        public void setLastPaidAt(Instant lastPaidAt) { this.lastPaidAt = lastPaidAt; }
    }
}
