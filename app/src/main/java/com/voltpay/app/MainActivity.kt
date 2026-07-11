package com.voltpay.app

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voltpay.app.data.db.TransactionDao
import com.voltpay.app.data.model.Transaction
import com.voltpay.app.ui.analytics.AnalyticsActivity
import com.voltpay.app.ui.balance.BalanceActivity
import com.voltpay.app.ui.contacts.ContactsActivity
import com.voltpay.app.ui.history.HistoryActivity
import com.voltpay.app.ui.payment.SendMoneyActivity
import com.voltpay.app.ui.request.RequestActivity
import com.voltpay.app.ui.scan.MyQrActivity
import com.voltpay.app.ui.scan.ScanQrActivity
import com.voltpay.app.ui.settings.SettingsActivity
import java.io.IOException
import java.security.GeneralSecurityException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var transactionDao: TransactionDao
    private var securePrefs: SharedPreferences? = null
    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        transactionDao = TransactionDao(this)

        rvRecentTransactions = findViewById(R.id.rvRecentTransactions)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        
        rvRecentTransactions.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.btnHeaderMyQr)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, MyQrActivity::class.java))
        }

        findViewById<View>(R.id.btnActionRequest)?.setOnClickListener {
            checkUpiIdAndProceed(RequestActivity::class.java)
        }
        


        initSecurePrefs()

        findViewById<View>(R.id.btnActionPay)?.setOnClickListener {
            checkUpiIdAndProceed(SendMoneyActivity::class.java)
        }

        findViewById<View>(R.id.btnActionContacts)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, ContactsActivity::class.java))
        }

        findViewById<View>(R.id.btnActionScan)?.setOnClickListener {
            checkUpiIdAndProceed(ScanQrActivity::class.java)
        }

        findViewById<View>(R.id.tvViewAll)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, HistoryActivity::class.java))
        }

        findViewById<View>(R.id.btnActionAnalytics)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, AnalyticsActivity::class.java))
        }

        findViewById<View>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }

        findViewById<View>(R.id.btnActionBalance)?.setOnClickListener {
            startActivity(Intent(this@MainActivity, BalanceActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecentTransactions()
        loadUserProfile()
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
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun checkUpiIdAndProceed(targetActivity: Class<*>) {
        if (securePrefs == null) return
        
        val savedUpi = securePrefs?.getString("user_upi_id", "")
        if (savedUpi.isNullOrEmpty()) {
            showLazyUpiIdSheet(targetActivity)
        } else {
            startActivity(Intent(this, targetActivity))
        }
    }

    private fun showLazyUpiIdSheet(targetActivity: Class<*>) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_upi_id, null)
        dialog.setContentView(view)
        
        val etUpiId = view.findViewById<EditText>(R.id.etUpiId)
        val btnSave = view.findViewById<Button>(R.id.btnSaveUpi)
        
        val phone = securePrefs?.getString("user_phone_number", "")
        if (!phone.isNullOrEmpty()) {
            etUpiId.setText("$phone@")
            etUpiId.setSelection(etUpiId.text.length)
        }

        btnSave.setOnClickListener {
            val upi = etUpiId.text?.toString()?.trim() ?: ""
            if (!upi.matches(Regex("[a-zA-Z0-9._-]+@[a-zA-Z]+"))) {
                etUpiId.error = "Invalid UPI ID format"
                return@setOnClickListener
            }
            securePrefs?.edit()?.putString("user_upi_id", upi)?.apply()
            dialog.dismiss()
            startActivity(Intent(this, targetActivity))
        }
        
        dialog.show()
    }

    private fun loadUserProfile() {
        val name = securePrefs?.getString("user_name", "User") ?: "User"
        findViewById<TextView>(R.id.tvGreeting)?.text = "Hi, $name"
    }

    private fun loadRecentTransactions() {
        val allTransactions = transactionDao.getAllTransactions()
        val previewList = if (allTransactions.size > 3) allTransactions.subList(0, 3) else allTransactions

        if (previewList.isEmpty()) {
            rvRecentTransactions.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            rvRecentTransactions.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            
            adapter = TransactionAdapter(previewList)
            rvRecentTransactions.adapter = adapter
        }
    }

    private class TransactionAdapter(private val transactions: List<Transaction>) : RecyclerView.Adapter<TransactionAdapter.ViewHolder>() {
        private val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction_preview, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val tx = transactions[position]
            
            holder.tvCounterparty.text = tx.counterparty ?: "Unknown"
            holder.tvTimestamp.text = dateFormat.format(Date(tx.timestamp))
            
            if (tx.type.equals("CREDIT", ignoreCase = true)) {
                holder.tvAmount.text = String.format(Locale.getDefault(), "+ ₹%.2f", tx.amount)
                holder.tvAmount.setTextColor(Color.parseColor("#10B981")) // Emerald green
                holder.ivIcon.setBackgroundColor(Color.parseColor("#10B981"))
            } else {
                holder.tvAmount.text = String.format(Locale.getDefault(), "- ₹%.2f", tx.amount)
                holder.tvAmount.setTextColor(Color.parseColor("#0F172A")) // Dark Slate
                holder.ivIcon.setBackgroundColor(Color.parseColor("#0F172A"))
            }
        }

        override fun getItemCount(): Int = transactions.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvCounterparty: TextView = itemView.findViewById(R.id.tvCounterparty)
            val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
            val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
            val ivIcon: View = itemView.findViewById(R.id.ivTransactionIcon)
        }
    }
}
