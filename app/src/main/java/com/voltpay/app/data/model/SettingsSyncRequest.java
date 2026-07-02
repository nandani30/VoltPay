package com.voltpay.app.data.model;

public class SettingsSyncRequest {
    private String displayName;
    private String ownUpiId;
    private Integer simSlotIndex;

    public SettingsSyncRequest(String displayName, String ownUpiId, Integer simSlotIndex) {
        this.displayName = displayName;
        this.ownUpiId = ownUpiId;
        this.simSlotIndex = simSlotIndex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getOwnUpiId() {
        return ownUpiId;
    }

    public void setOwnUpiId(String ownUpiId) {
        this.ownUpiId = ownUpiId;
    }

    public Integer getSimSlotIndex() {
        return simSlotIndex;
    }

    public void setSimSlotIndex(Integer simSlotIndex) {
        this.simSlotIndex = simSlotIndex;
    }
}
