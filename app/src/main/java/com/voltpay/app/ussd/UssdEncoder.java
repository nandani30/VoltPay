package com.voltpay.app.ussd;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

public class UssdEncoder {

    public static String encodeUssd(String ussdCode) {
        if (ussdCode == null) return "";
        return ussdCode.replace("#", "%23");
    }

    public static String buildSendMoneyInitialString() {
        return "*99#";
    }

    public static String buildCheckBalanceString() {
        return "*99*3#";
    }

    /**
     * Builds a single-stage string for sending money to a UPI ID.
     * Note: This is a best-effort TRAI standard fallback string. 
     * If the bank rejects this, the accessibility service will fall back to manual menu navigation.
     */
    public static String buildSingleStageSendMoneyUpi(String upiId, double amount) {
        // e.g. *99*1*3*test@ybl*100#
        return "*99*1*3*" + upiId + "*" + (int) amount + "#";
    }

    /**
     * Builds a single-stage string for sending money to a phone number.
     */
    public static String buildSingleStageSendMoneyPhone(String phone, double amount) {
        // e.g. *99*1*1*9876543210*100#
        return "*99*1*1*" + phone + "*" + (int) amount + "#";
    }

    public interface UssdCallback {
        void onResponse(String text);
        void onFailure(String error);
    }

    /**
     * Executes a USSD request using the official TelephonyManager API (API 26+).
     * This is an alternative to the accessibility service for checking balance or handling short responses.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void executeUssdRequest(Context context, String ussdCode, UssdCallback callback) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            callback.onFailure("TelephonyManager is unavailable");
            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            callback.onFailure("CALL_PHONE permission is required");
            return;
        }

        telephonyManager.sendUssdRequest(ussdCode, new TelephonyManager.UssdResponseCallback() {
            @Override
            public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                callback.onResponse(response.toString());
            }

            @Override
            public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                String errorMsg = "USSD Request Failed (Code: " + failureCode + ")";
                callback.onFailure(errorMsg);
            }
        }, new Handler(Looper.getMainLooper()));
    }
}
