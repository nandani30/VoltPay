package com.voltpay.app.ui.payment

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.voltpay.app.R
import com.voltpay.app.ui.contacts.ContactsActivity

class SendMoneyActivity : AppCompatActivity() {

    private lateinit var etAmount: EditText
    private lateinit var etUpiId: EditText
    private lateinit var btnPay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // SECURITY: FLAG_SECURE prevents screen recording and screenshots
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        
        setContentView(R.layout.activity_send_money)

        etAmount = findViewById(R.id.etAmount)
        etUpiId = findViewById(R.id.etUpiId)
        btnPay = findViewById(R.id.btnPay)

        btnPay.setOnClickListener { handlePayment() }
        
        findViewById<TextView>(R.id.tvSelectContact)?.setOnClickListener {
            val intent = Intent(this, ContactsActivity::class.java)
            startActivity(intent)
            finish() // Close SendMoneyActivity so back button from Contacts returns to Dashboard
        }
    }

    private fun handlePayment() {
        val amountStr = etAmount.text.toString().trim()
        var upiId = etUpiId.text.toString().trim()

        if (amountStr.isEmpty() || upiId.isEmpty()) {
            Toast.makeText(this, "Please enter all fields correctly", Toast.LENGTH_SHORT).show()
            return
        }

        // Clean up the UPI ID/Phone Number
        if (!upiId.contains("@")) {
            upiId = upiId.replace("\\s+".toRegex(), "") // Remove all spaces
            if (upiId.startsWith("+91")) {
                upiId = upiId.substring(3)
            } else if (upiId.startsWith("0") && upiId.length == 11) {
                upiId = upiId.substring(1)
            }
            etUpiId.setText(upiId) // Update UI with cleaned number
        }

        val amount = try {
            amountStr.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidUpiId(upiId) && !isValidMobileNumber(upiId)) {
            Toast.makeText(this, "Please enter a valid UPI ID (e.g. name@bank) or 10-digit mobile number", Toast.LENGTH_LONG).show()
            return
        }

        if (amount <= 0 || amount > 100000) {
            Toast.makeText(this, "Amount must be between ₹1 and ₹1,00,000", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, PaymentProcessingActivity::class.java)
        intent.putExtra("EXTRA_UPI_ID", upiId)
        intent.putExtra("EXTRA_AMOUNT", amount)
        startActivity(intent)
        finish()
    }

    private fun isValidUpiId(upiId: String?): Boolean {
        return upiId != null && upiId.matches(Regex("^[a-zA-Z0-9.\\-_]{3,256}@[a-zA-Z]{2,64}$"))
    }

    private fun isValidMobileNumber(input: String?): Boolean {
        return input != null && input.matches(Regex("^[6-9]\\d{9}$"))
    }
}
