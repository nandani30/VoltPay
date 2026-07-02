package com.voltpay.app.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.voltpay.app.R;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class PinLengthSetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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

            boolean isSettingsMode = getIntent().getBooleanExtra("from_settings", false);
            if (!isSettingsMode && sharedPreferences.contains("pin_length")) {
                startActivity(new Intent(this, OTPVerificationActivity.class));
                finish();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_pin_length_setup);

        findViewById(R.id.card4Digit).setOnClickListener(v -> savePinLength(4));
        findViewById(R.id.card6Digit).setOnClickListener(v -> savePinLength(6));
    }

    @Override
    public void onBackPressed() {
        boolean isSettingsMode = getIntent().getBooleanExtra("from_settings", false);
        if (isSettingsMode) {
            super.onBackPressed();
        } else {
            moveTaskToBack(true);
        }
    }

    private void savePinLength(int length) {
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

            sharedPreferences.edit().putInt("pin_length", length).apply();
            
            boolean isSettingsMode = getIntent().getBooleanExtra("from_settings", false);
            if (isSettingsMode) {
                finish();
            } else {
                Intent intent = new Intent(this, OTPVerificationActivity.class);
                startActivity(intent);
                finish();
            }

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to securely save settings", Toast.LENGTH_SHORT).show();
        }
    }
}
