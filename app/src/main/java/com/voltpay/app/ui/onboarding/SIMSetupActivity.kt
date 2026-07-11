package com.voltpay.app.ui.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.voltpay.app.R
import com.voltpay.app.utils.SimUtils
import java.io.IOException
import java.security.GeneralSecurityException

class SIMSetupActivity : AppCompatActivity() {

    private lateinit var tvJioWarning: View
    private lateinit var llManualPhone: View
    private lateinit var etManualPhone: EditText
    private lateinit var btnContinue: Button
    private var selectedSimSubscriptionId = -1
    private var selectedPhoneNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sim_setup)

        tvJioWarning = findViewById(R.id.tvJioWarning)
        llManualPhone = findViewById(R.id.llManualPhone)
        etManualPhone = findViewById(R.id.etManualPhone)
        btnContinue = findViewById(R.id.btnContinue)

        btnContinue.setOnClickListener {
            if (selectedSimSubscriptionId != -1) {
                if (llManualPhone.visibility == View.VISIBLE) {
                    var manualPhone = etManualPhone.text.toString().trim()
                    if (!manualPhone.startsWith("+")) {
                        manualPhone = "+91$manualPhone"
                    }
                    if (manualPhone.length < 13) {
                        etManualPhone.error = "Enter a valid 10-digit Indian phone number"
                        return@setOnClickListener
                    }
                    selectedPhoneNumber = manualPhone
                }

                saveSelectedSim(selectedSimSubscriptionId, selectedPhoneNumber)

                val isSettingsMode = intent.getBooleanExtra("from_settings", false)
                if (isSettingsMode) {
                    finish()
                } else {
                    val intent = Intent(this, ProfileSetupActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }

        checkPermissionsAndLoadSims()
    }

    override fun onBackPressed() {
        val isSettingsMode = intent.getBooleanExtra("from_settings", false)
        if (isSettingsMode) {
            super.onBackPressed()
        } else {
            moveTaskToBack(true)
        }
    }

    private fun checkPermissionsAndLoadSims() {
        val permissions = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE
        )

        var allGranted = true
        for (perm in permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false
                break
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        } else {
            loadSims()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            if (allGranted) {
                loadSims()
            } else {
                Toast.makeText(this, "Permissions are required to detect SIMs.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadSims() {
        val simListContainer = findViewById<LinearLayout>(R.id.simListContainer)
        simListContainer.removeAllViews()

        val sims = SimUtils.getActiveSims(this)

        if (sims.isEmpty()) {
            Toast.makeText(this, "No SIM card detected. VoltPay requires a SIM card for payments.", Toast.LENGTH_LONG).show()
        } else {
            val radioGroup = RadioGroup(this)
            radioGroup.orientation = RadioGroup.VERTICAL

            for (sim in sims) {
                val rb = RadioButton(this)
                val displayPhone = if (!sim.phoneNumber.isNullOrEmpty()) " (${sim.phoneNumber})" else ""
                rb.text = "SIM ${sim.slotIndex + 1} - ${sim.carrierName}$displayPhone"
                rb.textSize = 16f
                rb.setTextColor(0xFF212121.toInt())
                rb.setPadding(0, 16, 0, 16)
                rb.id = sim.subscriptionId

                rb.setOnClickListener {
                    selectedSimSubscriptionId = sim.subscriptionId
                    selectedPhoneNumber = sim.phoneNumber ?: ""
                    btnContinue.isEnabled = true

                    if (selectedPhoneNumber.isEmpty()) {
                        llManualPhone.visibility = View.VISIBLE
                    } else {
                        llManualPhone.visibility = View.GONE
                    }

                    if (sim.isJio) {
                        tvJioWarning.visibility = View.VISIBLE
                    } else {
                        tvJioWarning.visibility = View.GONE
                    }
                }

                radioGroup.addView(rb)
            }
            simListContainer.addView(radioGroup)
        }
    }

    private fun saveSelectedSim(subscriptionId: Int, phoneNumber: String) {
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
                .putInt("selected_sim_subscription_id", subscriptionId)
                .putString("user_phone_number", phoneNumber)
                .apply()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to securely save settings", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
    }
}
