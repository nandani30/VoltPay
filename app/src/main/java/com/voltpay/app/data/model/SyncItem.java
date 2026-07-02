package com.voltpay.app.data.model;

public class SyncItem {
    private long id;
    private String actionType;
    private String payload;
    private long createdAt;
    private int retryCount;
    private long lastAttempted;
    private String status; // PENDING, PROCESSING, FAILED

    public SyncItem() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public long getLastAttempted() { return lastAttempted; }
    public void setLastAttempted(long lastAttempted) { this.lastAttempted = lastAttempted; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
