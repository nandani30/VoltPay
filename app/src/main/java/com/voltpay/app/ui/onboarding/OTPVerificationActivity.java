package com.voltpay.app.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.messaging.FirebaseMessaging;
import com.voltpay.app.BuildConfig;
import com.voltpay.app.R;
import com.voltpay.app.data.api.VoltPayApi;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OTPVerificationActivity extends AppCompatActivity {

    private String phoneNumber;
    private String name;
    private VoltPayApi api;
    private FirebaseAuth mAuth;
    private String mVerificationId;
    
    private Button btnVerify;
    private EditText etOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        mAuth = FirebaseAuth.getInstance();

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

        btnVerify = findViewById(R.id.btnVerify);
        etOtp = findViewById(R.id.etOtp);
        
        btnVerify.setEnabled(false); // Disable until code sent
        
        btnVerify.setOnClickListener(v -> {
            String otp = etOtp.getText().toString().trim();
            if (otp.length() == 6 && mVerificationId != null) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otp);
                signInWithPhoneAuthCredential(credential);
            } else {
                Toast.makeText(this, "Enter valid 6-digit OTP", Toast.LENGTH_SHORT).show();
            }
        });

        startPhoneNumberVerification(phoneNumber);
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(this)                 // Activity (for callback binding)
                        .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // Auto-retrieval succeeded
                    if (credential.getSmsCode() != null) {
                        etOtp.setText(credential.getSmsCode());
                    }
                    signInWithPhoneAuthCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Log.e("PhoneAuth", "Verification failed", e);
                    Toast.makeText(OTPVerificationActivity.this, "Verification Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    mVerificationId = verificationId;
                    btnVerify.setEnabled(true);
                    Toast.makeText(OTPVerificationActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
                }
            };

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        task.getResult().getUser().getIdToken(true).addOnCompleteListener(tokenTask -> {
                            if (tokenTask.isSuccessful()) {
                                String idToken = tokenTask.getResult().getToken();
                                registerWithBackend(idToken);
                            } else {
                                Toast.makeText(this, "Failed to get Firebase token", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(OTPVerificationActivity.this, "Invalid OTP", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerWithBackend(String firebaseIdToken) {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            String fcmToken = task.isSuccessful() && task.getResult() != null ? task.getResult() : "";

            Map<String, String> body = new HashMap<>();
            body.put("firebaseIdToken", firebaseIdToken);
            body.put("phoneNumber", phoneNumber);
            body.put("name", name);
            body.put("fcmToken", fcmToken);

            api.verifyFirebaseTokenAndRegister(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        String authToken = (String) response.body().get("authToken");
                        saveAuthTokenAndContinue(authToken);
                    } else {
                        Toast.makeText(OTPVerificationActivity.this, "Registration failed on server", Toast.LENGTH_SHORT).show();
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
