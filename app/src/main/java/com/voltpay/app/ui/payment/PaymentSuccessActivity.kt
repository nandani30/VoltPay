package com.voltpay.app.ui.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.voltpay.app.MainActivity
import com.voltpay.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentSuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_success)

        val amount = intent.getDoubleExtra("amount", 0.0)
        val recipientName = intent.getStringExtra("recipientName")
        val recipientUpiId = intent.getStringExtra("recipientUpiId")
        val upiRef = intent.getStringExtra("upiRef")
        val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())
        var source = intent.getStringExtra("source")
        
        if (source == null) {
            source = "MANUAL"
        }
        
        val isAuto = intent.getBooleanExtra("isAuto", false)

        val tvAmount = findViewById<TextView>(R.id.tvAmount)
        val tvCounterparty = findViewById<TextView>(R.id.tvCounterparty)
        val tvCounterpartyUpi = findViewById<TextView>(R.id.tvCounterpartyUpi)
        val tvRefNumber = findViewById<TextView>(R.id.tvRefNumber)
        val tvSourceBadge = findViewById<TextView>(R.id.tvSourceBadge)
        val tvDateTime = findViewById<TextView>(R.id.tvDateTime)
        val btnCopyRef = findViewById<View>(R.id.btnCopyRef)
        val btnDone = findViewById<Button>(R.id.btnDone)

        tvAmount.text = String.format(Locale.getDefault(), "₹%.2f", amount)
        
        if (recipientName != null && recipientName != recipientUpiId && recipientName != "Unknown") {
            tvCounterparty.text = recipientName
            tvCounterpartyUpi.text = recipientUpiId
        } else {
            tvCounterparty.text = recipientUpiId ?: "Unknown"
            tvCounterpartyUpi.text = ""
        }

        tvRefNumber.text = upiRef ?: "N/A"
        tvSourceBadge.text = if (isAuto) "AUTO" else "MANUAL"
        
        val sdf = SimpleDateFormat("EEE, dd MMM hh:mm a", Locale.getDefault())
        tvDateTime.text = sdf.format(Date(timestamp))

        btnCopyRef.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("UPI Reference", upiRef ?: "N/A")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Reference copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        btnDone.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
