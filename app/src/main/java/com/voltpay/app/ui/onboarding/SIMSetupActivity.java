package com.voltpay.app.ui.onboarding;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.voltpay.app.R;
import com.voltpay.app.utils.SimUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class SIMSetupActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;
    
    private View tvJioWarning;
    private View llManualPhone;
    private android.widget.EditText etManualPhone;
    private Button btnContinue;
    private int selectedSimSubscriptionId = -1;
    private String selectedPhoneNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sim_setup);

        tvJioWarning = findViewById(R.id.tvJioWarning);
        llManualPhone = findViewById(R.id.llManualPhone);
        etManualPhone = findViewById(R.id.etManualPhone);
        btnContinue = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> {
            if (selectedSimSubscriptionId != -1) {
                if (llManualPhone.getVisibility() == View.VISIBLE) {
                    String manualPhone = etManualPhone.getText().toString().trim();
                    if (!manualPhone.startsWith("+")) {
                        manualPhone = "+91" + manualPhone;
                    }
                    if (manualPhone.length() < 13) {
                        etManualPhone.setError("Enter a valid 10-digit Indian phone number");
                        return;
                    }
                    selectedPhoneNumber = manualPhone;
                }
                
                saveSelectedSim(selectedSimSubscriptionId, selectedPhoneNumber);
                
                boolean isSettingsMode = getIntent().getBooleanExtra("from_settings", false);
                if (isSettingsMode) {
                    finish();
                } else {
                    Intent intent = new Intent(this, ProfileSetupActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });

        checkPermissionsAndLoadSims();
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

    private void checkPermissionsAndLoadSims() {
        String[] permissions = {
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE
        };

        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            loadSims();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                loadSims();
            } else {
                Toast.makeText(this, "Permissions are required to detect SIMs.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadSims() {
        LinearLayout simListContainer = findViewById(R.id.simListContainer);
        simListContainer.removeAllViews();
        
        List<SimUtils.SimInfo> sims = SimUtils.getActiveSims(this);
        
        if (sims.isEmpty()) {
            Toast.makeText(this, "No SIM card detected. VoltPay requires a SIM card for payments.", Toast.LENGTH_LONG).show();
        } else {
            RadioGroup radioGroup = new RadioGroup(this);
            radioGroup.setOrientation(RadioGroup.VERTICAL);
            
            for (SimUtils.SimInfo sim : sims) {
                RadioButton rb = new RadioButton(this);
                String displayPhone = sim.phoneNumber != null && !sim.phoneNumber.isEmpty() ? " (" + sim.phoneNumber + ")" : "";
                rb.setText("SIM " + (sim.slotIndex + 1) + " - " + sim.carrierName + displayPhone);
                rb.setTextSize(16);
                rb.setTextColor(0xFF212121);
                rb.setPadding(0, 16, 0, 16);
                rb.setId(sim.subscriptionId);
                
                rb.setOnClickListener(v -> {
                    selectedSimSubscriptionId = sim.subscriptionId;
                    selectedPhoneNumber = sim.phoneNumber != null ? sim.phoneNumber : "";
                    btnContinue.setEnabled(true);
                    
                    if (selectedPhoneNumber.isEmpty()) {
                        llManualPhone.setVisibility(View.VISIBLE);
                    } else {
                        llManualPhone.setVisibility(View.GONE);
                    }
                    
                    if (sim.isJio) {
                        tvJioWarning.setVisibility(View.VISIBLE);
                    } else {
                        tvJioWarning.setVisibility(View.GONE);
                    }
                });
                
                radioGroup.addView(rb);
            }
            simListContainer.addView(radioGroup);
        }
    }

    private void saveSelectedSim(int subscriptionId, String phoneNumber) {
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
                    .putInt("selected_sim_subscription_id", subscriptionId)
                    .putString("user_phone_number", phoneNumber)
                    .apply();
            
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to securely save settings", Toast.LENGTH_SHORT).show();
        }
    }
}
