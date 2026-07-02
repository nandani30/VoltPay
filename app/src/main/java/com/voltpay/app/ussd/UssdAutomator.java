package com.voltpay.app.ussd;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class UssdAutomator {

    private final Context context;

    public UssdAutomator(Context context) {
        this.context = context;
    }

    /**
     * Dials a USSD string manually using the SIM selected during onboarding.
     * 
     * @param rawUssdString The USSD string (e.g. "*99*1*3#")
     */
    public void dialManualUssd(String rawUssdString) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Call Phone permission required for USSD", Toast.LENGTH_SHORT).show();
            return;
        }

        String encodedHash = UssdEncoder.encodeUssd(rawUssdString);
        Uri uri = Uri.parse("tel:" + encodedHash);
        Intent intent = new Intent(Intent.ACTION_CALL, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Retrieve the selected SIM subscription ID from encrypted prefs
        int simSubscriptionId = getSelectedSimId();

        if (simSubscriptionId != -1) {
            // Bind the intent to the specific SIM card
            TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null) {
                List<PhoneAccountHandle> phoneAccounts = telecomManager.getCallCapablePhoneAccounts();
                for (PhoneAccountHandle handle : phoneAccounts) {
                    if (handle.getId().contains(String.valueOf(simSubscriptionId))) {
                        intent.putExtra("android.telecom.extra.PHONE_ACCOUNT_HANDLE", handle);
                        break;
                    }
                }
            }
        }

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Failed to launch USSD dialer.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private int getSelectedSimId() {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    "voltpay_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            return sharedPreferences.getInt("selected_sim_subscription_id", -1);
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
