package com.voltpay.app.ui.request

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.gson.Gson
import com.voltpay.app.BuildConfig
import com.voltpay.app.R
import com.voltpay.app.data.api.VoltPayApi
import com.voltpay.app.data.db.SyncItemDao
import com.voltpay.app.data.model.SyncItem
import com.voltpay.app.sync.SyncWorker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RequestActivity : AppCompatActivity() {

    private lateinit var etTargetPhone: EditText
    private lateinit var etAmount: EditText
    private lateinit var etNote: EditText
    private lateinit var etMyUpiId: EditText
    private lateinit var btnSendRequest: Button
    private lateinit var pbLoading: ProgressBar
    private var userPhone: String = ""
    private var userName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        etTargetPhone = findViewById(R.id.etTargetPhone)
        etAmount = findViewById(R.id.etAmount)
        etNote = findViewById(R.id.etNote)
        etMyUpiId = findViewById(R.id.etMyUpiId)
        btnSendRequest = findViewById(R.id.btnSendRequest)
        pbLoading = findViewById(R.id.pbLoading)

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

            etMyUpiId.setText(prefs.getString("user_upi", ""))
            userPhone = prefs.getString("user_phone", "") ?: ""
            userName = prefs.getString("user_name", "VoltPay User") ?: "VoltPay User"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        btnSendRequest.setOnClickListener { sendRequest() }
    }

    private fun sendRequest() {
        val targetPhone = etTargetPhone.text.toString().trim()
        val amountStr = etAmount.text.toString().trim()
        val note = etNote.text.toString().trim()
        val myUpi = etMyUpiId.text.toString().trim()

        if (targetPhone.length != 10) {
            etTargetPhone.error = "Enter a valid 10-digit number"
            return
        }
        if (amountStr.isEmpty()) {
            etAmount.error = "Enter amount"
            return
        }
        val amount = amountStr.toDoubleOrNull() ?: 0.0
        if (amount <= 0 || amount > 5000) {
            etAmount.error = "Amount must be between 1 and 5000"
            return
        }
        if (myUpi.isEmpty()) {
            etMyUpiId.error = "UPI ID is required"
            return
        }

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        val isConnected = activeNetwork?.isConnectedOrConnecting == true

        if (!isConnected) {
            Toast.makeText(this, "No internet connection. Request will be sent when you are online.", Toast.LENGTH_LONG).show()
            
            val syncItemDao = SyncItemDao(this)
            val payload = mapOf(
                "requesterPhone" to userPhone,
                "requesterName" to userName,
                "requesterUpiId" to myUpi,
                "payerPhone" to targetPhone,
                "amount" to amount,
                "note" to note
            )
            
            val newItem = SyncItem()
            newItem.actionType = "SEND_REQUEST"
            newItem.payload = Gson().toJson(payload)
            newItem.createdAt = System.currentTimeMillis()
            newItem.status = "PENDING"
            syncItemDao.insertSyncItem(newItem)
            
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val syncRequest = OneTimeWorkRequest.Builder(SyncWorker::class.java)
                .setConstraints(constraints).build()
            WorkManager.getInstance(this).enqueue(syncRequest)

            finish()
            return
        }

        btnSendRequest.text = ""
        pbLoading.visibility = View.VISIBLE
        btnSendRequest.isEnabled = false

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(VoltPayApi::class.java)
        
        val body = mapOf(
            "requesterPhone" to userPhone,
            "requesterName" to userName,
            "requesterUpiId" to myUpi,
            "payerPhone" to targetPhone,
            "amount" to amount,
            "note" to note
        )

        var token = ""
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
            token = prefs.getString("auth_token", "") ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val authToken = if (token.isEmpty()) "" else "Bearer $token"

        api.createRequest(authToken, body).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                pbLoading.visibility = View.GONE
                btnSendRequest.text = "Send Request"
                btnSendRequest.isEnabled = true

                if (response.isSuccessful) {
                    Toast.makeText(this@RequestActivity, "Request sent successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    if (response.code() == 404) {
                        Toast.makeText(this@RequestActivity, "User does not have VoltPay installed.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@RequestActivity, "Failed to send request. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                pbLoading.visibility = View.GONE
                btnSendRequest.text = "Send Request"
                btnSendRequest.isEnabled = true
                Toast.makeText(this@RequestActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
