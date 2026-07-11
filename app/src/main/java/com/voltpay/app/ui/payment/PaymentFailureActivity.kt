package com.voltpay.app.ui.payment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.voltpay.app.MainActivity
import com.voltpay.app.R
import java.util.Locale
import kotlin.math.floor

class PaymentFailureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_failure)

        val errorMessage = intent.getStringExtra("errorMessage")
        val amount = intent.getDoubleExtra("amount", 0.0)
        val recipientUpiId = intent.getStringExtra("recipientUpiId")

        val tvErrorMessage = findViewById<TextView>(R.id.tvErrorMessage)
        val tvAmount = findViewById<TextView>(R.id.tvAmount)
        val tvCounterpartyUpi = findViewById<TextView>(R.id.tvCounterpartyUpi)
        val btnTryAgain = findViewById<Button>(R.id.btnTryAgain)
        val btnHome = findViewById<Button>(R.id.btnHome)

        if (!errorMessage.isNullOrEmpty()) {
            tvErrorMessage.text = errorMessage
        }

        if (amount > 0) {
            tvAmount.text = String.format(Locale.getDefault(), "₹%.2f", amount)
        } else {
            tvAmount.visibility = View.GONE
        }

        if (!recipientUpiId.isNullOrEmpty() && recipientUpiId != "Unknown") {
            tvCounterpartyUpi.text = recipientUpiId
        } else {
            tvCounterpartyUpi.visibility = View.GONE
        }

        btnTryAgain.setOnClickListener {
            val intent = Intent(this, SendMoneyActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!recipientUpiId.isNullOrEmpty() && recipientUpiId != "Unknown") {
                intent.putExtra("EXTRA_UPI_ID", recipientUpiId)
            }
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.btnPayManually)?.setOnClickListener {
            if (!recipientUpiId.isNullOrEmpty() && recipientUpiId != "Unknown") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("UPI ID", recipientUpiId)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "UPI ID copied — paste it when the dialer asks", Toast.LENGTH_LONG).show()
                
                val amountStr = if (amount == floor(amount)) {
                    amount.toInt().toString()
                } else {
                    amount.toString()
                }
                
                val ussdString = "*99*1*3*$recipientUpiId*$amountStr#"
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = Uri.parse("tel:" + Uri.encode(ussdString))
                startActivity(dialIntent)
            } else {
                Toast.makeText(this, "UPI ID is missing.", Toast.LENGTH_SHORT).show()
            }
        }

        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
