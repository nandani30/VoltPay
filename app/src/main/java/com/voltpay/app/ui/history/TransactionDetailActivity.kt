package com.voltpay.app.ui.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.voltpay.app.R
import com.voltpay.app.data.db.TransactionDao
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_detail)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        val txId = intent.getLongExtra("transaction_id", -1)
        if (txId == -1L) {
            Toast.makeText(this, "Invalid transaction", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val dao = TransactionDao(this)
        val tx = dao.getById(txId)
        if (tx == null) {
            Toast.makeText(this, "Transaction not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val tvAmount = findViewById<TextView>(R.id.tvAmount)
        val tvToFromLabel = findViewById<TextView>(R.id.tvToFromLabel)
        val tvCounterpartyName = findViewById<TextView>(R.id.tvCounterpartyName)
        val tvCounterpartyUpi = findViewById<TextView>(R.id.tvCounterpartyUpi)
        val tvUpiRef = findViewById<TextView>(R.id.tvUpiRef)
        val tvDateTime = findViewById<TextView>(R.id.tvDateTime)
        val tvSource = findViewById<TextView>(R.id.tvSource)
        val btnCopyRef = findViewById<ImageView>(R.id.btnCopyRef)

        val isCredit = tx.type == "CREDIT"

        if (isCredit) {
            tvAmount.text = String.format(Locale.getDefault(), "+ ₹%.2f", tx.amount)
            tvAmount.setTextColor(Color.parseColor("#10B981"))
            tvToFromLabel.text = "From"
        } else {
            tvAmount.text = String.format(Locale.getDefault(), "- ₹%.2f", tx.amount)
            tvAmount.setTextColor(Color.parseColor("#F44336"))
            tvToFromLabel.text = "To"
        }

        val counterparty = tx.counterparty ?: "Unknown"
        tvCounterpartyName.text = counterparty
        tvCounterpartyUpi.text = counterparty
        
        tvUpiRef.text = tx.upiRef ?: "N/A"

        val fullDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy, hh:mm a", Locale.getDefault())
        tvDateTime.text = fullDateFormat.format(Date(tx.timestamp))

        if (tx.source.equals("AUTO", ignoreCase = true)) {
            tvSource.text = "Processed automatically"
        } else {
            tvSource.text = "Confirmed manually"
        }

        btnCopyRef.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("UPI Reference", tx.upiRef ?: "N/A")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
        }
    }
}
