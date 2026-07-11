package com.voltpay.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.voltpay.app.BuildConfig
import com.voltpay.app.MainActivity
import com.voltpay.app.R
import com.voltpay.app.data.api.VoltPayApi
import com.voltpay.app.data.db.ContactDao
import com.voltpay.app.data.model.Contact
import com.voltpay.app.data.model.ContactSyncResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SetupCompleteActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_complete)

        val tvUserName = findViewById<TextView>(R.id.tvUserName)

        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                this,
                "voltpay_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val name = sharedPreferences.getString("user_name", "")
            if (!name.isNullOrEmpty()) {
                tvUserName.text = "$name!"
            }

            sharedPreferences.edit().putBoolean("is_onboarding_complete", true).apply()

            val phone = sharedPreferences.getString("user_phone", "")
            val token = sharedPreferences.getString("auth_token", "")
            if (!phone.isNullOrEmpty() && !token.isNullOrEmpty()) {
                checkBackupAndRestore("Bearer $token", phone)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        val btnLetsGo = findViewById<Button>(R.id.btnLetsGo)
        btnLetsGo.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun checkBackupAndRestore(token: String, phoneNumber: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val api = retrofit.create(VoltPayApi::class.java)

        api.restoreContacts(token, phoneNumber).enqueue(object : Callback<ContactSyncResponse> {
            override fun onResponse(call: Call<ContactSyncResponse>, response: Response<ContactSyncResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val contacts = response.body()?.contacts
                    if (!contacts.isNullOrEmpty()) {
                        showRestoreDialog(contacts)
                    }
                }
            }

            override fun onFailure(call: Call<ContactSyncResponse>, t: Throwable) {
                // Silently ignore
            }
        })
    }

    private fun showRestoreDialog(contacts: List<Contact>) {
        AlertDialog.Builder(this)
            .setTitle("Restore Contacts")
            .setMessage("We found ${contacts.size} contacts from your previous device. Restore them?")
            .setPositiveButton("Yes") { _, _ ->
                val dao = ContactDao(this@SetupCompleteActivity)
                for (c in contacts) {
                    val existing = dao.getContactByUpiId(c.upiId ?: "")
                    if (existing == null) {
                        dao.insertContact(c)
                    } else if (c.createdAt > existing.createdAt) {
                        c.id = existing.id
                        dao.updateContact(c)
                    }
                }
                Toast.makeText(this@SetupCompleteActivity, "Contacts restored", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
