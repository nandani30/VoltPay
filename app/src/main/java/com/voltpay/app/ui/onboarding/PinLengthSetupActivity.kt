package com.voltpay.app.ui.onboarding

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.voltpay.app.BuildConfig
import com.voltpay.app.R
import com.voltpay.app.data.api.VoltPayApi
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PinLengthSetupActivity : AppCompatActivity() {

    private var phoneNumber = ""
    private var name = ""
    private lateinit var api: VoltPayApi
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(VoltPayApi::class.java)

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

            phoneNumber = sharedPreferences.getString("user_phone_number", "") ?: ""
            name = sharedPreferences.getString("user_name", "") ?: ""

            val isSettingsMode = intent.getBooleanExtra("from_settings", false)
            if (!isSettingsMode && sharedPreferences.contains("pin_length") && sharedPreferences.contains("auth_token")) {
                startActivity(Intent(this, SyncOptInActivity::class.java))
                finish()
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContentView(R.layout.activity_pin_length_setup)

        findViewById<android.view.View>(R.id.card4Digit).setOnClickListener { savePinLength(4) }
        findViewById<android.view.View>(R.id.card6Digit).setOnClickListener { savePinLength(6) }
    }

    override fun onBackPressed() {
        val isSettingsMode = intent.getBooleanExtra("from_settings", false)
        if (isSettingsMode) {
            super.onBackPressed()
        } else {
            moveTaskToBack(true)
        }
    }

    private fun savePinLength(length: Int) {
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

            sharedPreferences.edit().putInt("pin_length", length).apply()

            val isSettingsMode = intent.getBooleanExtra("from_settings", false)
            if (isSettingsMode) {
                finish()
            } else {
                registerWithBackend()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to securely save settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerWithBackend() {
        progressDialog = ProgressDialog(this).apply {
            setMessage("Registering securely...")
            setCancelable(false)
            show()
        }

        val body = HashMap<String, String>()
        body["phoneNumber"] = phoneNumber
        body["password"] = "default_password"
        body["name"] = name

        api.login(body).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                progressDialog?.takeIf { it.isShowing }?.dismiss()
                if (response.isSuccessful && response.body() != null) {
                    val authToken = response.body()?.get("token") as? String ?: ""
                    saveAuthTokenAndContinue(authToken)
                } else {
                    Toast.makeText(this@PinLengthSetupActivity, "Registration failed on server", Toast.LENGTH_SHORT).show()
                    // Allow continuing offline
                    saveAuthTokenAndContinue("offline_dummy_token")
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                progressDialog?.takeIf { it.isShowing }?.dismiss()
                Toast.makeText(this@PinLengthSetupActivity, "Network error", Toast.LENGTH_SHORT).show()
                // Allow continuing offline
                saveAuthTokenAndContinue("offline_dummy_token")
            }
        })
    }

    private fun saveAuthTokenAndContinue(authToken: String) {
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

            sharedPreferences.edit()
                .putString("auth_token", authToken)
                .putString("user_phone", phoneNumber)
                .apply()

            val intent = Intent(this, SyncOptInActivity::class.java)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
