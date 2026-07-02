package com.voltpay.app.ui.splash;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.voltpay.app.MainActivity;
import com.voltpay.app.ui.onboarding.OnboardingActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Root check MUST be first — before any navigation
        com.scottyab.rootbeer.RootBeer rootBeer = new com.scottyab.rootbeer.RootBeer(this);
        if (rootBeer.isRooted()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Security Warning")
                .setMessage("VoltPay cannot run on rooted devices for your security.")
                .setCancelable(false)
                .setPositiveButton("Exit", (dialog, which) -> finishAffinity())
                .show();
            return; // Stop here. Do not navigate anywhere.
        }

        boolean isOnboardingComplete = false;
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    this,
                    "voltpay_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            isOnboardingComplete = sharedPreferences.getBoolean("is_onboarding_complete", false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (isOnboardingComplete) {
            startActivity(new Intent(this, MainActivity.class));
        } else {
            startActivity(new Intent(this, OnboardingActivity.class));
        }
        finish();
    }
}
