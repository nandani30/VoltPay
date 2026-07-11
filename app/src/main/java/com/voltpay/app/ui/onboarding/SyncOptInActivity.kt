package com.voltpay.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.voltpay.app.R

class SyncOptInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sync_opt_in)

        val btnOptIn = findViewById<Button>(R.id.btnOptIn)
        val btnSkip = findViewById<Button>(R.id.btnSkip)

        btnOptIn.setOnClickListener {
            saveSyncPreference(true)
            proceed()
        }

        btnSkip.setOnClickListener {
            saveSyncPreference(false)
            proceed()
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private fun saveSyncPreference(enable: Boolean) {
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
                .putBoolean("cloud_sync_enabled", enable)
                .apply()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun proceed() {
        val intent = Intent(this, SetupCompleteActivity::class.java)
        startActivity(intent)
        finish()
    }
}
