package com.voltpay.app.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.voltpay.app.R;

public class SyncOptInActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_opt_in);

        Button btnOptIn = findViewById(R.id.btnOptIn);
        Button btnSkip = findViewById(R.id.btnSkip);

        btnOptIn.setOnClickListener(v -> {
            saveSyncPreference(true);
            proceed();
        });

        btnSkip.setOnClickListener(v -> {
            saveSyncPreference(false);
            proceed();
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void saveSyncPreference(boolean enable) {
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

            sharedPreferences.edit()
                    .putBoolean("cloud_sync_enabled", enable)
                    .apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void proceed() {
        Intent intent = new Intent(this, SetupCompleteActivity.class);
        startActivity(intent);
        finish();
    }
}
