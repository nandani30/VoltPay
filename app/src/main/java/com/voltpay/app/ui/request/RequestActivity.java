package com.voltpay.app.ui.request;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.voltpay.app.BuildConfig;
import com.voltpay.app.R;
import com.voltpay.app.data.api.VoltPayApi;
import com.voltpay.app.data.db.SyncItemDao;
import com.voltpay.app.data.model.SyncItem;
import com.voltpay.app.sync.SyncWorker;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RequestActivity extends AppCompatActivity {

    private EditText etTargetPhone, etAmount, etNote, etMyUpiId;
    private Button btnSendRequest;
    private ProgressBar pbLoading;
    private String userPhone, userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etTargetPhone = findViewById(R.id.etTargetPhone);
        etAmount = findViewById(R.id.etAmount);
        etNote = findViewById(R.id.etNote);
        etMyUpiId = findViewById(R.id.etMyUpiId);
        btnSendRequest = findViewById(R.id.btnSendRequest);
        pbLoading = findViewById(R.id.pbLoading);

        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    "voltpay_secure_prefs", masterKeyAlias, this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            etMyUpiId.setText(prefs.getString("user_upi", ""));
            userPhone = prefs.getString("user_phone", "");
            userName = prefs.getString("user_name", "VoltPay User");
        } catch (Exception e) {
            e.printStackTrace();
        }

        btnSendRequest.setOnClickListener(v -> sendRequest());
    }

    private void sendRequest() {
        String targetPhone = etTargetPhone.getText().toString().trim();
        String amountStr = etAmount.getText().toString().trim();
        String note = etNote.getText().toString().trim();
        String myUpi = etMyUpiId.getText().toString().trim();

        if (targetPhone.length() != 10) {
            etTargetPhone.setError("Enter a valid 10-digit number");
            return;
        }
        if (amountStr.isEmpty()) {
            etAmount.setError("Enter amount");
            return;
        }
        double amount = Double.parseDouble(amountStr);
        if (amount <= 0 || amount > 5000) {
            etAmount.setError("Amount must be between 1 and 5000");
            return;
        }
        if (myUpi.isEmpty()) {
            etMyUpiId.setError("UPI ID is required");
            return;
        }

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        if (!isConnected) {
            Toast.makeText(this, "No internet connection. Request will be sent when you are online.", Toast.LENGTH_LONG).show();
            
            SyncItemDao syncItemDao = new SyncItemDao(this);
            Map<String, Object> payload = new HashMap<>();
            payload.put("requesterPhone", userPhone);
            payload.put("requesterName", userName);
            payload.put("requesterUpiId", myUpi);
            payload.put("payerPhone", targetPhone);
            payload.put("amount", amount);
            payload.put("note", note);
            
            SyncItem newItem = new SyncItem();
            newItem.setActionType("SEND_REQUEST");
            newItem.setPayload(new Gson().toJson(payload));
            newItem.setCreatedAt(System.currentTimeMillis());
            newItem.setStatus("PENDING");
            syncItemDao.insertSyncItem(newItem);
            
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                    .setConstraints(constraints).build();
            WorkManager.getInstance(this).enqueue(syncRequest);

            finish();
            return;
        }

        btnSendRequest.setText("");
        pbLoading.setVisibility(View.VISIBLE);
        btnSendRequest.setEnabled(false);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BACKEND_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        VoltPayApi api = retrofit.create(VoltPayApi.class);
        
        Map<String, Object> body = new HashMap<>();
        body.put("requesterPhone", userPhone);
        body.put("requesterName", userName);
        body.put("requesterUpiId", myUpi);
        body.put("payerPhone", targetPhone);
        body.put("amount", amount);
        body.put("note", note);

        SharedPreferences prefs = null;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(
                    "voltpay_secure_prefs", masterKeyAlias, this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        String token = prefs != null ? prefs.getString("auth_token", "") : "";
        String authToken = token.isEmpty() ? "" : "Bearer " + token;

        api.createRequest(authToken, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                pbLoading.setVisibility(View.GONE);
                btnSendRequest.setText("Send Request");
                btnSendRequest.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(RequestActivity.this, "Request sent successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    if (response.code() == 404) {
                        Toast.makeText(RequestActivity.this, "User does not have VoltPay installed.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(RequestActivity.this, "Failed to send request. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                pbLoading.setVisibility(View.GONE);
                btnSendRequest.setText("Send Request");
                btnSendRequest.setEnabled(true);
                Toast.makeText(RequestActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
