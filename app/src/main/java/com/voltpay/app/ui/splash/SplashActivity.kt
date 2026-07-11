package com.voltpay.app.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.scottyab.rootbeer.RootBeer
import com.voltpay.app.MainActivity
import com.voltpay.app.ui.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Root check MUST be first — before any navigation
        val rootBeer = RootBeer(this)
        if (rootBeer.isRooted) {
            AlertDialog.Builder(this)
                .setTitle("Security Warning")
                .setMessage("VoltPay cannot run on rooted devices for your security.")
                .setCancelable(false)
                .setPositiveButton("Exit") { _, _ -> finishAffinity() }
                .show()
            return // Stop here. Do not navigate anywhere.
        }

        var isOnboardingComplete = false
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
            isOnboardingComplete = sharedPreferences.getBoolean("is_onboarding_complete", false)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (isOnboardingComplete) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
        finish()
    }
}
