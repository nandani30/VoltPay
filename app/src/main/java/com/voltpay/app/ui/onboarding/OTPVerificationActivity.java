package com.voltpay.app.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.firebase.messaging.FirebaseMessaging;
import com.voltpay.app.BuildConfig;
import com.voltpay.app.R;
import com.voltpay.app.data.api.VoltPayApi;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OTPVerificationActivity extends AppCompatActivity {

    private String phoneNumber;
    private String name;
    private VoltPayApi api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BACKEND_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(VoltPayApi.class);

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

            phoneNumber = sharedPreferences.getString("user_phone_number", "");
            name = sharedPreferences.getString("user_name", "");

        } catch (Exception e) {
            e.printStackTrace();
        }

        requestOtp();

        Button btnVerify = findViewById(R.id.btnVerify);
        EditText etOtp = findViewById(R.id.etOtp);
        
        btnVerify.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.length() == 6) {
                verifyOtp(otp);
            } else {
                Toast.makeText(this, "Enter valid 6-digit OTP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void requestOtp() {
        Map<String, String> body = new HashMap<>();
        body.put("phoneNumber", phoneNumber);

        api.requestOtp(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // For local testing, print the mock OTP
                    if (BuildConfig.DEBUG && response.body().containsKey("mockOtp")) {
                        Toast.makeText(OTPVerificationActivity.this,
                            "Dev Mock OTP: " + response.body().get("mockOtp"),
                            Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(OTPVerificationActivity.this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(OTPVerificationActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyOtp(String otp) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            String fcmToken = task.isSuccessful() && task.getResult() != null ? task.getResult() : "";

            Map<String, String> body = new HashMap<>();
            body.put("phoneNumber", phoneNumber);
            body.put("otp", otp);
            body.put("name", name);
            body.put("fcmToken", fcmToken);

            api.verifyOtpAndRegister(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String authToken = (String) response.body().get("authToken");
                        saveAuthTokenAndContinue(authToken);
                    } else {
                        Toast.makeText(OTPVerificationActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Toast.makeText(OTPVerificationActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void saveAuthTokenAndContinue(String authToken) {
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
                    .putString("auth_token", authToken)
                    .putString("user_phone", phoneNumber)
                    .apply();

            Intent intent = new Intent(this, SyncOptInActivity.class);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
