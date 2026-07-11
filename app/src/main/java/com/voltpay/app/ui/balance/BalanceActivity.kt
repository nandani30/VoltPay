package com.voltpay.app.ui.balance

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.voltpay.app.BuildConfig
import com.voltpay.app.R
import com.voltpay.app.engine.UssdResultCallback
import com.voltpay.app.engine.VoltPayEngineWrapper
import com.voltpay.app.utils.AccessibilityUtils
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.Locale
import java.util.regex.Pattern

class BalanceActivity : AppCompatActivity() {

    private lateinit var tvBalanceAmount: TextView
    private lateinit var tvJioWarning: View
    private lateinit var pbLoading: ProgressBar
    private lateinit var llBalanceContent: LinearLayout
    private lateinit var llManualInput: LinearLayout

    private var securePrefs: SharedPreferences? = null
    private var isAutoMode = true
    private var waitingForManualReturn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance)

        tvBalanceAmount = findViewById(R.id.tvBalanceAmount)
        tvJioWarning = findViewById(R.id.tvJioWarning)
        pbLoading = findViewById(R.id.pbLoading)
        llBalanceContent = findViewById(R.id.llBalanceContent)
        llManualInput = findViewById(R.id.llManualInput)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        initSecurePrefs()
        isAutoMode = securePrefs?.getBoolean("pref_auto_mode", true) ?: true

        val jioStatus = checkJioSimStatus()
        if (jioStatus == 1) { // Only Jio
            AlertDialog.Builder(this)
                .setTitle("Jio Network Detected")
                .setMessage("Your Jio SIM doesn't support *99# offline balance checks.")
                .setPositiveButton("OK") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        } else if (jioStatus == 2) { // Dual SIM
            AlertDialog.Builder(this)
                .setTitle("Jio Network Detected")
                .setMessage("Your Jio SIM doesn't support *99#. Switch to your other SIM and try again.")
                .setPositiveButton("Proceed Anyway") { _, _ -> startBalanceCheck() }
                .setNegativeButton("Cancel") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        } else {
            startBalanceCheck()
        }

        findViewById<View>(R.id.btnSaveManualBalance).setOnClickListener {
            val etManual = findViewById<EditText>(R.id.etManualBalance)
            val valStr = etManual.text.toString()
            if (valStr.isNotEmpty()) {
                try {
                    saveBalanceAndRefreshUI(valStr.toDouble())
                    llManualInput.visibility = View.GONE
                    Toast.makeText(this, "Balance saved", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun initSecurePrefs() {
        try {
            val masterKey = MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            securePrefs = EncryptedSharedPreferences.create(
                this,
                "voltpay_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveBalanceAndRefreshUI(amount: Double) {
        val now = System.currentTimeMillis()
        securePrefs?.edit()
            ?.putFloat("last_balance", amount.toFloat())
            ?.putLong("last_balance_timestamp", now)
            ?.apply()

        pbLoading.visibility = View.GONE
        llBalanceContent.visibility = View.VISIBLE

        tvBalanceAmount.text = String.format(Locale.getDefault(), "₹%.2f", amount)
    }

    private fun startBalanceCheck() {
        llBalanceContent.visibility = View.INVISIBLE
        pbLoading.visibility = View.VISIBLE
        llManualInput.visibility = View.GONE

        if (!AccessibilityUtils.isAccessibilityServiceEnabled(this)) {
            pbLoading.visibility = View.GONE
            llManualInput.visibility = View.VISIBLE
            AccessibilityUtils.promptToEnableAccessibility(this)
            return
        }

        if (BuildConfig.DEBUG) {
            // Debug/emulator only — fake balance after 2s
            Handler(Looper.getMainLooper()).postDelayed({
                saveBalanceAndRefreshUI(12450.75)
            }, 2000)
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                pbLoading.visibility = View.GONE
                llManualInput.visibility = View.VISIBLE
                Toast.makeText(this, "Phone call permission is required to check balance automatically.", Toast.LENGTH_LONG).show()
                return
            }

            VoltPayEngineWrapper.initialize(this)
            VoltPayEngineWrapper.checkBalance(object : UssdResultCallback {
                override fun onSuccess(message: String) {
                    runOnUiThread {
                        try {
                            val bal = extractBalance(message)
                            saveBalanceAndRefreshUI(bal)
                        } catch (e: Exception) {
                            pbLoading.visibility = View.GONE
                            llManualInput.visibility = View.VISIBLE
                            Toast.makeText(this@BalanceActivity, "Could not parse balance from: $message", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onError(error: String, carrierText: String) {
                    runOnUiThread {
                        if (pbLoading.visibility == View.VISIBLE) {
                            pbLoading.visibility = View.GONE
                            llManualInput.visibility = View.VISIBLE
                            Toast.makeText(this@BalanceActivity, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        if (waitingForManualReturn) {
            waitingForManualReturn = false
            pbLoading.visibility = View.GONE
            llBalanceContent.visibility = View.VISIBLE
            llManualInput.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        VoltPayEngineWrapper.cancel()
    }

    private fun checkJioSimStatus(): Int {
        try {
            val sm = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            if (sm != null) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    var hasJio = false
                    var hasNonJio = false
                    val activeSubscriptionInfoList = sm.activeSubscriptionInfoList
                    if (activeSubscriptionInfoList != null) {
                        for (info in activeSubscriptionInfoList) {
                            val carrier = info.carrierName?.toString()?.toLowerCase(Locale.getDefault()) ?: ""
                            if (carrier.contains("jio") || carrier.contains("reliance")) {
                                hasJio = true
                            } else if (carrier.isNotEmpty()) {
                                hasNonJio = true
                            }
                        }
                    }
                    if (hasJio && hasNonJio) return 2
                    if (hasJio) return 1
                }
            }
        } catch (e: Exception) {
        }
        return 0
    }

    private fun extractBalance(text: String): Double {
        val m = Pattern.compile("(?i)(?:rs\\.?|inr|₹|balance is)\\s*([\\d,]+\\.?\\d*)").matcher(text)
        if (m.find()) {
            return m.group(1)?.replace(",", "")?.toDouble() ?: 0.0
        }
        throw IllegalArgumentException("No balance found in text")
    }

    companion object {
        const val ACTION_BALANCE_RECEIVED = "com.voltpay.app.BALANCE_RECEIVED"
    }
}
