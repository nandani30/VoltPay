package com.voltpay.app.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import android.widget.EditText;
import com.voltpay.app.R;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class ProfileSetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        EditText etName = findViewById(R.id.etName);
        Button btnContinue = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";

            if (name.length() < 2) {
                etName.setError("Name must be at least 2 characters");
                return;
            }

            saveProfile(name);
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void saveProfile(String name) {
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
                    .putString("user_name", name)
                    .apply();
            
            // Navigate to Pin Length Setup next
            Intent intent = new Intent(this, PinLengthSetupActivity.class);
            startActivity(intent);
            finish();

        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to securely save profile", Toast.LENGTH_SHORT).show();
        }
    }
}
