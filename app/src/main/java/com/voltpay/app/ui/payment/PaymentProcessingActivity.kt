package com.voltpay.app.ui.payment

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.voltpay.app.R
import com.voltpay.app.engine.UssdResultCallback
import com.voltpay.app.engine.VoltPayEngineWrapper
import com.voltpay.app.utils.AccessibilityUtils
import java.util.Locale

class PaymentProcessingActivity : AppCompatActivity() {

    private var resultReceived = false
    private var originalAmount: Double = 0.0
    private var recipientUpi: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!AccessibilityUtils.isAccessibilityServiceEnabled(this)) {
            AccessibilityUtils.promptToEnableAccessibility(this)
            finish()
            return
        }

        setContentView(R.layout.activity_payment_processing)

        recipientUpi = intent.getStringExtra("EXTRA_UPI_ID")
        originalAmount = intent.getDoubleExtra("EXTRA_AMOUNT", 0.0)

        findViewById<Button>(R.id.btnCancelPayment)?.setOnClickListener {
            cancelPayment(originalAmount, recipientUpi)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            checkJioAndDial(recipientUpi, originalAmount)
        }, 500)
    }

    private fun checkJioAndDial(upiId: String?, amount: Double) {
        val jioStatus = checkJioSimStatus()
        if (jioStatus == 1) {
            AlertDialog.Builder(this)
                .setTitle("Jio Network Detected")
                .setMessage("Your Jio SIM doesn't support *99# offline payments. You cannot proceed.")
                .setPositiveButton("OK") { _, _ -> cancelPayment(amount, upiId) }
                .setCancelable(false)
                .show()
        } else if (jioStatus == 2) {
            AlertDialog.Builder(this)
                .setTitle("Jio Network Detected")
                .setMessage("Your Jio SIM doesn't support *99#. Switch to your other SIM and try again.")
                .setPositiveButton("Proceed Anyway") { _, _ -> startKotlinEngine(upiId, amount) }
                .setNegativeButton("Cancel") { _, _ -> cancelPayment(amount, upiId) }
                .setCancelable(false)
                .show()
        } else {
            startKotlinEngine(upiId, amount)
        }
    }

    private fun startKotlinEngine(upiId: String?, amount: Double) {
        if (upiId == null) {
            handleFailure("Missing UPI ID", amount, upiId)
            return
        }
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            handleFailure("Phone call permission is required to process offline payments.", amount, upiId)
            return
        }

        VoltPayEngineWrapper.initialize(this)
        VoltPayEngineWrapper.sendMoney(upiId, amount, object : UssdResultCallback {
            override fun onSuccess(message: String) {
                if (resultReceived) return
                resultReceived = true
                runOnUiThread {
                    val successIntent = Intent(this@PaymentProcessingActivity, PaymentSuccessActivity::class.java)
                    successIntent.putExtra("amount", amount)
                    successIntent.putExtra("recipientUpiId", upiId)
                    startActivity(successIntent)
                    finish()
                }
            }

            override fun onError(error: String, carrierText: String) {
                if (resultReceived) return
                resultReceived = true
                runOnUiThread {
                    handleFailure("$error: $carrierText", amount, upiId)
                }
            }
        })
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
        } catch (e: Exception) {}
        return 0
    }

    private fun cancelPayment(amount: Double, upiId: String?) {
        resultReceived = true
        VoltPayEngineWrapper.cancel()
        handleFailure("Payment was cancelled.", amount, upiId)
    }

    private fun handleFailure(message: String, amount: Double, upiId: String?) {
        val failureIntent = Intent(this, PaymentFailureActivity::class.java)
        failureIntent.putExtra("errorMessage", message)
        failureIntent.putExtra("amount", amount)
        failureIntent.putExtra("recipientUpiId", upiId)
        startActivity(failureIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!resultReceived) {
            VoltPayEngineWrapper.cancel()
        }
    }
}
