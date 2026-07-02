package com.voltpay.app.ui.balance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.voltpay.app.R;
import com.voltpay.app.ussd.UssdAutomator;
import com.voltpay.app.utils.BalanceHolder;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Locale;

public class BalanceActivity extends AppCompatActivity {

    public static final String ACTION_BALANCE_RECEIVED = "com.voltpay.app.BALANCE_RECEIVED";

    private TextView tvBalanceAmount;
    private View tvJioWarning;
    private ProgressBar pbLoading;
    private LinearLayout llBalanceContent, llManualInput;
    
    private SharedPreferences securePrefs;
    private boolean isAutoMode = true;
    private boolean waitingForManualReturn = false;

    private final BroadcastReceiver balanceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_BALANCE_RECEIVED.equals(intent.getAction())) {
                double balance = BalanceHolder.getInstance().getAndClearBalance();
                saveBalanceAndRefreshUI(balance);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance);

        tvBalanceAmount = findViewById(R.id.tvBalanceAmount);

        tvJioWarning = findViewById(R.id.tvJioWarning);
        pbLoading = findViewById(R.id.pbLoading);
        llBalanceContent = findViewById(R.id.llBalanceContent);
        llManualInput = findViewById(R.id.llManualInput);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        initSecurePrefs();
        isAutoMode = securePrefs.getBoolean("pref_auto_mode", true);

        LocalBroadcastManager.getInstance(this).registerReceiver(balanceReceiver, new IntentFilter(ACTION_BALANCE_RECEIVED));

        if (isJioSim()) {
            tvJioWarning.setVisibility(View.VISIBLE);
        } else {
            startBalanceCheck();
        }

        findViewById(R.id.btnSaveManualBalance).setOnClickListener(v -> {
            EditText etManual = findViewById(R.id.etManualBalance);
            String val = etManual.getText().toString();
            if (!val.isEmpty()) {
                try {
                    saveBalanceAndRefreshUI(Double.parseDouble(val));
                    llManualInput.setVisibility(View.GONE);
                    Toast.makeText(this, "Balance saved", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {}
            }
        });
    }

    private void initSecurePrefs() {
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            securePrefs = EncryptedSharedPreferences.create(
                    this,
                    "voltpay_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }



    private void saveBalanceAndRefreshUI(double amount) {
        long now = System.currentTimeMillis();
        if (securePrefs != null) {
            securePrefs.edit()
                    .putFloat("last_balance", (float) amount)
                    .putLong("last_balance_timestamp", now)
                    .apply();
        }
        
        pbLoading.setVisibility(View.GONE);
        llBalanceContent.setVisibility(View.VISIBLE);
        
        tvBalanceAmount.setText(String.format(Locale.getDefault(), "₹%.2f", amount));
    }

    private void startBalanceCheck() {
        llBalanceContent.setVisibility(View.INVISIBLE);
        pbLoading.setVisibility(View.VISIBLE);
        llManualInput.setVisibility(View.GONE);
        
        if (com.voltpay.app.BuildConfig.DEBUG) {
            // Debug/emulator only — fake balance after 2s
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                saveBalanceAndRefreshUI(12450.75);
            }, 2000);
        } else {
            // Production: dial *99*3# and wait for accessibility service broadcast
            com.voltpay.app.utils.UssdSessionHolder.getInstance().setBalanceSession();
            new UssdAutomator(this).dialManualUssd(
                com.voltpay.app.ussd.UssdEncoder.buildCheckBalanceString()
            );

            // 30-second timeout: if no broadcast received, show manual input
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (pbLoading.getVisibility() == View.VISIBLE) {
                    pbLoading.setVisibility(View.GONE);
                    llManualInput.setVisibility(View.VISIBLE);
                    com.voltpay.app.utils.UssdSessionHolder.getInstance().clearSession();
                    Toast.makeText(BalanceActivity.this,
                        "Could not read balance automatically. Please enter it manually.",
                        Toast.LENGTH_LONG).show();
                }
            }, 30000);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (waitingForManualReturn) {
            waitingForManualReturn = false;
            pbLoading.setVisibility(View.GONE);
            llBalanceContent.setVisibility(View.VISIBLE);
            llManualInput.setVisibility(View.VISIBLE);
        }
    }

    private boolean isJioSim() {
        try {
            SubscriptionManager sm = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (sm != null) {
                // In production, require READ_PHONE_STATE permission check here
                try {
                    for (SubscriptionInfo info : sm.getActiveSubscriptionInfoList()) {
                        String carrier = info.getCarrierName() != null ? info.getCarrierName().toString().toLowerCase() : "";
                        if (carrier.contains("jio") || carrier.contains("reliance")) {
                            return true;
                        }
                    }
                } catch (SecurityException ignored) {}
            }
        } catch (Exception e) {}
        return false;
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(balanceReceiver);
        BalanceHolder.getInstance().setExpectingBalance(false);
    }
}
