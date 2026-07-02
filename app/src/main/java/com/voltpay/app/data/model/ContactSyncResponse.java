package com.voltpay.app.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ContactSyncResponse {
    private boolean success;
    @SerializedName("lastSyncedAt")
    private long syncedAt;
    private List<Contact> contacts;

    public ContactSyncResponse() {}

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(long syncedAt) {
        this.syncedAt = syncedAt;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(List<Contact> contacts) {
        this.contacts = contacts;
    }
}
