package com.voltpay.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.voltpay.app.R

class ProfileSetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_setup)

        val etName = findViewById<EditText>(R.id.etName)
        val btnContinue = findViewById<Button>(R.id.btnContinue)

        btnContinue.setOnClickListener {
            val name = etName.text?.toString()?.trim() ?: ""

            if (name.length < 2) {
                etName.error = "Name must be at least 2 characters"
                return@setOnClickListener
            }

            saveProfile(name)
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private fun saveProfile(name: String) {
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
                .putString("user_name", name)
                .apply()

            val intent = Intent(this, PinLengthSetupActivity::class.java)
            startActivity(intent)
            finish()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to securely save profile", Toast.LENGTH_SHORT).show()
        }
    }
}
