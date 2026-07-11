package com.voltpay.app.ui.request

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.voltpay.app.BuildConfig
import com.voltpay.app.R
import com.voltpay.app.data.api.VoltPayApi
import com.voltpay.app.ui.payment.SendMoneyActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class IncomingRequestActivity : AppCompatActivity() {

    private var requestId: String? = null
    private var requesterName: String? = null
    private var requesterUpiId: String? = null
    private var requesterPhone: String? = null
    private var amountStr: String? = null
    private var note: String? = null
    private var timestamp: Long = 0
    private lateinit var api: VoltPayApi
    private var myPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_request)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        val tvRequesterName = findViewById<TextView>(R.id.tvRequesterName)
        val tvRequesterUpiId = findViewById<TextView>(R.id.tvRequesterUpiId)
        val tvAmount = findViewById<TextView>(R.id.tvAmount)
        val tvNote = findViewById<TextView>(R.id.tvNote)
        val tvExpiredMessage = findViewById<TextView>(R.id.tvExpiredMessage)
        val btnPayNow = findViewById<Button>(R.id.btnPayNow)
        val btnDecline = findViewById<Button>(R.id.btnDecline)

        requestId = intent.getStringExtra("requestId")
        requesterName = intent.getStringExtra("requesterName")
        requesterUpiId = intent.getStringExtra("requesterUpiId")
        requesterPhone = intent.getStringExtra("requesterPhone")
        amountStr = intent.getStringExtra("amount")
        note = intent.getStringExtra("note")
        
        timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())

        tvRequesterName.text = requesterName ?: "Unknown"
        tvRequesterUpiId.text = requesterUpiId ?: ""
        tvAmount.text = if (amountStr != null) "₹$amountStr" else "₹0.00"
        
        if (!note.isNullOrEmpty()) {
            tvNote.visibility = View.VISIBLE
            tvNote.text = "\"$note\""
        }

        if (System.currentTimeMillis() - timestamp > 24 * 60 * 60 * 1000L) {
            btnPayNow.isEnabled = false
            btnPayNow.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFBDBDBD.toInt())
            tvExpiredMessage.visibility = View.VISIBLE
        }

        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                this,
                "voltpay_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            myPhone = prefs.getString("user_phone", "") ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(VoltPayApi::class.java)

        btnPayNow.setOnClickListener { payNow() }
        btnDecline.setOnClickListener { declineRequest() }
    }

    private fun payNow() {
        val body = mapOf("payerPhone" to myPhone)
        api.completeRequest(getAuthToken(), requestId ?: "", body).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {}
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {}
        })

        val intent = Intent(this, SendMoneyActivity::class.java).apply {
            putExtra("recipient_upi", requesterUpiId)
            putExtra("amount", amountStr)
        }
        startActivity(intent)
        finish()
    }

    private fun getAuthToken(): String {
        return try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                this,
                "voltpay_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val token = prefs.getString("auth_token", "") ?: ""
            if (token.isEmpty()) "" else "Bearer $token"
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun declineRequest() {
        val body = mapOf("payerPhone" to myPhone)
        api.declineRequest(getAuthToken(), requestId ?: "", body).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@IncomingRequestActivity, "Request declined", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@IncomingRequestActivity, "Failed to decline", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@IncomingRequestActivity, "Network error", Toast.LENGTH_SHORT).show()
                finish()
            }
        })
    }
}
