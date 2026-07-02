package com.voltpay.app.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;


import com.voltpay.app.R;

import java.io.IOException;
import java.security.GeneralSecurityException;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.voltpay.app.data.db.SyncItemDao;
import com.voltpay.app.data.model.SettingsSyncRequest;
import com.voltpay.app.data.model.SyncItem;
import com.voltpay.app.sync.SyncWorker;
import com.google.gson.Gson;

public class SettingsActivity extends AppCompatActivity {

    private EditText tvName, tvUpiId;
    private RadioGroup rgPinLength;
    private RadioButton rb4, rb6, rbUnknown;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tvName = findViewById(R.id.tvName);
        tvUpiId = findViewById(R.id.tvUpiId);
        
        rgPinLength = findViewById(R.id.rgPinLength);
        rb4 = findViewById(R.id.rb4);
        rb6 = findViewById(R.id.rb6);

        View rowSim = findViewById(R.id.rowSim);
        if (rowSim != null) {
            rowSim.setOnClickListener(v -> {
                Intent intent = new Intent(this, com.voltpay.app.ui.onboarding.SIMSetupActivity.class);
                intent.putExtra("from_settings", true);
                startActivity(intent);
            });
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        loadSettings();

        findViewById(R.id.btnSave).setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            sharedPreferences = EncryptedSharedPreferences.create(
                    this,
                    "voltpay_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            tvName.setText(sharedPreferences.getString("user_name", ""));
            tvUpiId.setText(sharedPreferences.getString("user_upi_id", ""));
            
            int pinLength = sharedPreferences.getInt("pin_length", -1);
            if (pinLength == 4) {
                rb4.setChecked(true);
            } else if (pinLength == 6) {
                rb6.setChecked(true);
            }
            
            TextView tvSim = findViewById(R.id.tvSim);
            if (tvSim != null) {
                String phone = sharedPreferences.getString("user_phone_number", "");
                if (phone.isEmpty()) {
                    tvSim.setText("Active");
                } else {
                    tvSim.setText(phone);
                }
            }
            
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSettings() {
        String name = tvName.getText().toString().trim();
        String upi = tvUpiId.getText().toString().trim();

        if (name.length() < 2) {
            tvName.setError("Name must be at least 2 characters");
            return;
        }

        if (!upi.matches("[a-zA-Z0-9._-]+@[a-zA-Z]+")) {
            tvUpiId.setError("Invalid UPI ID format");
            return;
        }

        int pinLength = sharedPreferences.getInt("pin_length", 6);
        if (rgPinLength != null) {
            if (rgPinLength.getCheckedRadioButtonId() == R.id.rb4) {
                pinLength = 4;
            } else if (rgPinLength.getCheckedRadioButtonId() == R.id.rb6) {
                pinLength = 6;
            }
        }

        if (sharedPreferences != null) {
            String oldUpi = sharedPreferences.getString("user_upi_id", "");
            
            sharedPreferences.edit()
                    .putString("user_name", name)
                    .putString("user_upi_id", upi)
                    .putInt("pin_length", pinLength)
                    .apply();
            
            if (!upi.equals(oldUpi)) {
                queueSyncSettings();
            }
            
            Toast.makeText(this, "Settings saved securely", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void queueSyncSettings() {
        SyncItemDao syncItemDao = new SyncItemDao(this);
        long currentTime = System.currentTimeMillis();
        
        String name = tvName.getText().toString().trim();
        String upi = tvUpiId.getText().toString().trim();
        int simSlotIndex = sharedPreferences.getInt("sim_slot_index", 0);
        SettingsSyncRequest request = new SettingsSyncRequest(name, upi, simSlotIndex);
        String payloadJson = new Gson().toJson(request);

        SyncItem newItem = new SyncItem();
        newItem.setActionType("SYNC_SETTINGS");
        newItem.setPayload(payloadJson);
        newItem.setCreatedAt(currentTime);
        newItem.setStatus("PENDING");
        syncItemDao.insertSyncItem(newItem);

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueue(syncRequest);
    }
}
