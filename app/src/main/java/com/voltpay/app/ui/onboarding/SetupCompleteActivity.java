package com.voltpay.app.ui.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.voltpay.app.MainActivity;
import com.voltpay.app.R;
import com.voltpay.app.BuildConfig;
import com.voltpay.app.data.api.VoltPayApi;
import com.voltpay.app.data.db.ContactDao;
import com.voltpay.app.data.model.Contact;
import com.voltpay.app.data.model.ContactSyncResponse;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.json.JSONObject;

import com.google.firebase.messaging.FirebaseMessaging;
import com.voltpay.app.data.db.SyncItemDao;
import com.voltpay.app.data.model.SyncItem;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import android.widget.Toast;

public class SetupCompleteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_complete);

        TextView tvUserName = findViewById(R.id.tvUserName);

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

            String name = sharedPreferences.getString("user_name", "");
            if (!name.isEmpty()) {
                tvUserName.setText(name + "!");
            }
            
            sharedPreferences.edit().putBoolean("is_onboarding_complete", true).apply();

            String phone = sharedPreferences.getString("user_phone", "");
            String token = sharedPreferences.getString("auth_token", "");
            if (!phone.isEmpty() && !token.isEmpty()) {
                checkBackupAndRestore("Bearer " + token, phone);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        Button btnLetsGo = findViewById(R.id.btnLetsGo);
        btnLetsGo.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void checkBackupAndRestore(String token, String phoneNumber) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.BACKEND_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        VoltPayApi api = retrofit.create(VoltPayApi.class);
        
        api.restoreContacts(token, phoneNumber).enqueue(new Callback<ContactSyncResponse>() {
            @Override
            public void onResponse(Call<ContactSyncResponse> call, Response<ContactSyncResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Contact> contacts = response.body().getContacts();
                    if (contacts != null && !contacts.isEmpty()) {
                        showRestoreDialog(contacts);
                    }
                }
            }
            @Override
            public void onFailure(Call<ContactSyncResponse> call, Throwable t) {
                // Silently ignore
            }
        });
    }

    private void showRestoreDialog(List<Contact> contacts) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Restore Contacts")
            .setMessage("We found " + contacts.size() + " contacts from your previous device. Restore them?")
            .setPositiveButton("Yes", (dialog, which) -> {
                ContactDao dao = new ContactDao(SetupCompleteActivity.this);
                for (Contact c : contacts) {
                    Contact existing = dao.getContactByUpiId(c.getUpiId());
                    if (existing == null) {
                        dao.insertContact(c);
                    } else if (c.getCreatedAt() > existing.getCreatedAt()) {
                        c.setId(existing.getId());
                        dao.updateContact(c);
                    }
                }
                Toast.makeText(SetupCompleteActivity.this, "Contacts restored", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("No", null)
            .show();
    }


}
