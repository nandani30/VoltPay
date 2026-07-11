package com.voltpay.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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

class OTPVerificationActivity : AppCompatActivity() {

    private var phoneNumber: String = ""
    private var name: String = ""
    private lateinit var api: VoltPayApi

    private lateinit var btnVerify: Button
    private lateinit var etOtp: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp_verification)

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

        } catch (e: Exception) {
            e.printStackTrace()
        }

        btnVerify = findViewById(R.id.btnVerify)
        etOtp = findViewById(R.id.etOtp)
        
        // Mock sending OTP
        Toast.makeText(this, "Mock OTP sent! Enter any 6 digits.", Toast.LENGTH_SHORT).show()

        btnVerify.setOnClickListener {
            val otp = etOtp.text.toString().trim()
            if (otp.length == 6) {
                // In a real app, you would verify the OTP here before proceeding.
                // Since this is a dummy flow without Firebase, any 6-digit OTP works.
                registerWithBackend()
            } else {
                Toast.makeText(this, "Enter valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerWithBackend() {
        val body = HashMap<String, String>()
        body["phoneNumber"] = phoneNumber
        body["password"] = "default_password" // Mock password for non-firebase auth
        body["name"] = name

        api.login(body).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful && response.body() != null) {
                    val authToken = response.body()?.get("token") as? String ?: "dummy_token"
                    saveAuthTokenAndContinue(authToken)
                } else {
                    // Fallback to local offline mode if server is unavailable or fails
                    Toast.makeText(this@OTPVerificationActivity, "Proceeding offline", Toast.LENGTH_SHORT).show()
                    saveAuthTokenAndContinue("offline_dummy_token")
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@OTPVerificationActivity, "Network error. Proceeding offline.", Toast.LENGTH_SHORT).show()
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
                .putString("user_password", "default_password")
                .apply()

            val intent = Intent(this, SyncOptInActivity::class.java)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
