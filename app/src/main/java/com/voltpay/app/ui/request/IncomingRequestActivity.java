package com.voltpay.app.ui.request;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.voltpay.app.BuildConfig;
import com.voltpay.app.R;
import com.voltpay.app.data.api.VoltPayApi;
import com.voltpay.app.ui.payment.SendMoneyActivity;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class IncomingRequestActivity extends AppCompatActivity {

    private String requestId, requesterName, requesterUpiId, requesterPhone, amountStr, note;
    private long timestamp = 0;
    private VoltPayApi api;
    private String myPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_request);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TextView tvRequesterName = findViewById(R.id.tvRequesterName);
        TextView tvRequesterUpiId = findViewById(R.id.tvRequesterUpiId);
        TextView tvAmount = findViewById(R.id.tvAmount);
        TextView tvNote = findViewById(R.id.tvNote);
        TextView tvExpiredMessage = findViewById(R.id.tvExpiredMessage);
        Button btnPayNow = findViewById(R.id.btnPayNow);
        Button btnDecline = findViewById(R.id.btnDecline);

        Intent intent = getIntent();
        requestId = intent.getStringExtra("requestId");
        requesterName = intent.getStringExtra("requesterName");
        requesterUpiId = intent.getStringExtra("requesterUpiId");
        requesterPhone = intent.getStringExtra("requesterPhone");
        amountStr = intent.getStringExtra("amount");
        note = intent.getStringExtra("note");
        
        // Use intent timestamp if available, else use current time (fallback)
        timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());

        tvRequesterName.setText(requesterName != null ? requesterName : "Unknown");
        tvRequesterUpiId.setText(requesterUpiId != null ? requesterUpiId : "");
        tvAmount.setText(amountStr != null ? "₹" + amountStr : "₹0.00");
        
        if (note != null && !note.isEmpty()) {
            tvNote.setVisibility(View.VISIBLE);
            tvNote.setText("\"" + note + "\"");
        }

        // Check if older than 24 hours
        if (System.currentTimeMillis() - timestamp > 24 * 60 * 60 * 1000L) {
            btnPayNow.setEnabled(false);
            btnPayNow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFBDBDBD));
            tvExpiredMessage.setVisibility(View.VISIBLE);
        }

        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    "voltpay_secure_prefs", masterKeyAlias, this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            myPhone = prefs.getString("user_phone", "");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BACKEND_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(VoltPayApi.class);

        btnPayNow.setOnClickListener(v -> payNow());
        btnDecline.setOnClickListener(v -> declineRequest());
    }

    private void payNow() {
        // Complete in background
        Map<String, String> body = new HashMap<>();
        body.put("payerPhone", myPhone);
        api.completeRequest(getAuthToken(), requestId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {}
            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {}
        });

        Intent intent = new Intent(this, SendMoneyActivity.class);
        intent.putExtra("recipient_upi", requesterUpiId);
        intent.putExtra("amount", amountStr);
        startActivity(intent);
        finish();
    }

    private String getAuthToken() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    "voltpay_secure_prefs", masterKeyAlias, this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            String token = prefs.getString("auth_token", "");
            return token.isEmpty() ? "" : "Bearer " + token;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void declineRequest() {
        Map<String, String> body = new HashMap<>();
        body.put("payerPhone", myPhone);
        api.declineRequest(getAuthToken(), requestId, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(IncomingRequestActivity.this, "Request declined", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(IncomingRequestActivity.this, "Failed to decline", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                Toast.makeText(IncomingRequestActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
