package com.voltpay.app.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.gson.Gson
import com.voltpay.app.R
import com.voltpay.app.data.db.SyncItemDao
import com.voltpay.app.data.model.SettingsSyncRequest
import com.voltpay.app.data.model.SyncItem
import com.voltpay.app.sync.SyncWorker
import com.voltpay.app.ui.onboarding.SIMSetupActivity
import java.io.IOException
import java.security.GeneralSecurityException

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvName: EditText
    private lateinit var tvUpiId: EditText
    private lateinit var rgPinLength: RadioGroup
    private lateinit var rb4: RadioButton
    private lateinit var rb6: RadioButton
    private var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        tvName = findViewById(R.id.tvName)
        tvUpiId = findViewById(R.id.tvUpiId)
        
        rgPinLength = findViewById(R.id.rgPinLength)
        rb4 = findViewById(R.id.rb4)
        rb6 = findViewById(R.id.rb6)

        val rowSim = findViewById<View>(R.id.rowSim)
        rowSim?.setOnClickListener {
            val intent = Intent(this, SIMSetupActivity::class.java)
            intent.putExtra("from_settings", true)
            startActivity(intent)
        }

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack?.setOnClickListener { finish() }

        loadSettings()

        findViewById<View>(R.id.btnSave).setOnClickListener { saveSettings() }
    }

    private fun loadSettings() {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            sharedPreferences = EncryptedSharedPreferences.create(
                this,
                "voltpay_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            tvName.setText(sharedPreferences?.getString("user_name", ""))
            tvUpiId.setText(sharedPreferences?.getString("user_upi_id", ""))
            
            val pinLength = sharedPreferences?.getInt("pin_length", -1) ?: -1
            if (pinLength == 4) {
                rb4.isChecked = true
            } else if (pinLength == 6) {
                rb6.isChecked = true
            }
            
            val tvSim = findViewById<TextView>(R.id.tvSim)
            if (tvSim != null) {
                val phone = sharedPreferences?.getString("user_phone_number", "") ?: ""
                if (phone.isEmpty()) {
                    tvSim.text = "Active"
                } else {
                    tvSim.text = phone
                }
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveSettings() {
        val name = tvName.text.toString().trim()
        val upi = tvUpiId.text.toString().trim()

        if (name.length < 2) {
            tvName.error = "Name must be at least 2 characters"
            return
        }

        if (!upi.matches(Regex("[a-zA-Z0-9._-]+@[a-zA-Z]+"))) {
            tvUpiId.error = "Invalid UPI ID format"
            return
        }

        var pinLength = sharedPreferences?.getInt("pin_length", 6) ?: 6
        if (rgPinLength.checkedRadioButtonId == R.id.rb4) {
            pinLength = 4
        } else if (rgPinLength.checkedRadioButtonId == R.id.rb6) {
            pinLength = 6
        }

        sharedPreferences?.let { prefs ->
            val oldUpi = prefs.getString("user_upi_id", "")
            
            prefs.edit()
                .putString("user_name", name)
                .putString("user_upi_id", upi)
                .putInt("pin_length", pinLength)
                .apply()
            
            if (upi != oldUpi) {
                queueSyncSettings()
            }
            
            Toast.makeText(this, "Settings saved securely", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun queueSyncSettings() {
        val syncItemDao = SyncItemDao(this)
        val currentTime = System.currentTimeMillis()
        
        val name = tvName.text.toString().trim()
        val upi = tvUpiId.text.toString().trim()
        val simSlotIndex = sharedPreferences?.getInt("sim_slot_index", 0) ?: 0
        val request = SettingsSyncRequest(name, upi, simSlotIndex)
        val payloadJson = Gson().toJson(request)

        val newItem = SyncItem()
        newItem.actionType = "SYNC_SETTINGS"
        newItem.payload = payloadJson
        newItem.createdAt = currentTime
        newItem.status = "PENDING"
        syncItemDao.insertSyncItem(newItem)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequest.Builder(SyncWorker::class.java)
            .setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueue(syncRequest)
    }
}
