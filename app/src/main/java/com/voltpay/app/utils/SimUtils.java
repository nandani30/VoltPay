package com.voltpay.app.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import androidx.core.app.ActivityCompat;
import java.util.ArrayList;
import java.util.List;

public class SimUtils {

    public static class SimInfo {
        public int subscriptionId;
        public int slotIndex;
        public String carrierName;
        public String phoneNumber;
        public boolean isJio;

        public SimInfo(int subscriptionId, int slotIndex, String carrierName, String phoneNumber) {
            this.subscriptionId = subscriptionId;
            this.slotIndex = slotIndex;
            this.carrierName = carrierName != null ? carrierName : "Unknown Carrier";
            this.phoneNumber = phoneNumber != null ? phoneNumber : "Unknown Number";
            
            String lowerCarrier = this.carrierName.toLowerCase();
            this.isJio = lowerCarrier.contains("jio") || lowerCarrier.contains("reliance");
        }
    }

    public static List<SimInfo> getActiveSims(Context context) {
        List<SimInfo> simList = new ArrayList<>();
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        
        if (subscriptionManager != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
                if (activeSubscriptionInfoList != null) {
                    for (SubscriptionInfo info : activeSubscriptionInfoList) {
                        SimInfo simInfo = new SimInfo(
                                info.getSubscriptionId(),
                                info.getSimSlotIndex(),
                                info.getCarrierName().toString(),
                                info.getNumber()
                        );
                        simList.add(simInfo);
                    }
                }
            }
        }
        return simList;
    }

    public static boolean isCarrierSupported(String carrierName) {
        if (carrierName == null) return false;
        String lower = carrierName.toLowerCase();
        // Jio does not support *99# USSD
        if (lower.contains("jio") || lower.contains("reliance")) {
            return false;
        }
        // Supported major carriers in India
        return lower.contains("airtel") || lower.contains("vi") || lower.contains("vodafone") || lower.contains("idea") || lower.contains("bsnl");
    }
}
