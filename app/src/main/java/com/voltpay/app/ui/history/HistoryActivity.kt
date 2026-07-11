package com.voltpay.app.ui.history

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.voltpay.app.R
import com.voltpay.app.data.db.TransactionDao
import com.voltpay.app.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var emptyStateLayout: View
    private lateinit var transactionDao: TransactionDao
    private lateinit var adapter: HistoryAdapter
    private var currentFilter = "ALL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        transactionDao = TransactionDao(this)
        
        rvHistory = findViewById(R.id.rvHistory)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        
        swipeRefresh.setColorSchemeColors(Color.parseColor("#2ED573"))
        swipeRefresh.setProgressBackgroundColorSchemeColor(Color.parseColor("#1A1A1A"))
        
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        rvHistory.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }

        val chipAll = findViewById<TextView>(R.id.chipAll)
        val chipSent = findViewById<TextView>(R.id.chipSent)
        val chipReceived = findViewById<TextView>(R.id.chipReceived)

        chipAll.setOnClickListener {
            updateChips(chipAll, chipSent, chipReceived)
            currentFilter = "ALL"
            loadHistory(currentFilter)
        }
        chipSent.setOnClickListener {
            updateChips(chipSent, chipAll, chipReceived)
            currentFilter = "DEBIT"
            loadHistory(currentFilter)
        }
        chipReceived.setOnClickListener {
            updateChips(chipReceived, chipAll, chipSent)
            currentFilter = "CREDIT"
            loadHistory(currentFilter)
        }

        swipeRefresh.setOnRefreshListener { loadHistory(currentFilter) }

        loadHistory(currentFilter)
    }

    private fun updateChips(selected: TextView, unselected1: TextView, unselected2: TextView) {
        selected.setTextColor(Color.parseColor("#FFFFFF"))
        selected.setBackgroundColor(Color.parseColor("#2ED573"))
        
        unselected1.setTextColor(Color.parseColor("#A0A0A0"))
        unselected1.setBackgroundColor(Color.parseColor("#1A1A1A"))
        
        unselected2.setTextColor(Color.parseColor("#A0A0A0"))
        unselected2.setBackgroundColor(Color.parseColor("#1A1A1A"))
    }

    private fun loadHistory(filter: String) {
        val transactions = if (filter == "DEBIT" || filter == "CREDIT") {
            transactionDao.getByType(filter)
        } else {
            transactionDao.getAllTransactions()
        }

        swipeRefresh.isRefreshing = false

        if (transactions.isEmpty()) {
            rvHistory.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            rvHistory.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            adapter = HistoryAdapter(transactions, object : OnTransactionClickListener {
                override fun onTransactionClick(transaction: Transaction) {
                    val intent = Intent(this@HistoryActivity, TransactionDetailActivity::class.java)
                    intent.putExtra("transaction_id", transaction.id)
                    startActivity(intent)
                }
            })
            rvHistory.adapter = adapter
        }
    }

    interface OnTransactionClickListener {
        fun onTransactionClick(transaction: Transaction)
    }

    private class HistoryAdapter(
        private val transactions: List<Transaction>,
        private val listener: OnTransactionClickListener
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        private val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction_preview, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val tx = transactions[position]
            
            holder.tvCounterparty.text = tx.counterparty ?: "Unknown"
            holder.tvUpiId.text = tx.counterparty
            holder.tvTimestamp.text = dateFormat.format(Date(tx.timestamp))
            
            if (tx.type == "CREDIT") {
                holder.tvAmount.text = "+ ₹${String.format(Locale.getDefault(), "%.2f", tx.amount)}"
                holder.tvAmount.setTextColor(Color.parseColor("#4ADE80")) // Emerald green
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_revert)
                holder.ivIcon.rotation = 90f
                holder.ivIcon.setColorFilter(Color.parseColor("#4ADE80"))
            } else {
                holder.tvAmount.text = "- ₹${String.format(Locale.getDefault(), "%.2f", tx.amount)}"
                holder.tvAmount.setTextColor(Color.parseColor("#FF4444")) // Slate dark
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_send)
                holder.ivIcon.rotation = 0f
                holder.ivIcon.setColorFilter(Color.parseColor("#FF4444"))
            }

            // Source badge
            holder.tvSourceBadge.text = tx.source?.uppercase() ?: "UNKNOWN"
            val bg = GradientDrawable()
            bg.cornerRadius = 16f
            when {
                tx.source.equals("AUTO", ignoreCase = true) -> bg.setColor(Color.parseColor("#2196F3"))
                tx.source.equals("MANUAL", ignoreCase = true) -> bg.setColor(Color.parseColor("#9E9E9E"))
                else -> bg.setColor(Color.parseColor("#9E9E9E"))
            }
            holder.tvSourceBadge.background = bg

            holder.itemView.setOnClickListener { listener.onTransactionClick(tx) }
        }

        override fun getItemCount(): Int = transactions.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvCounterparty: TextView = itemView.findViewById(R.id.tvCounterparty)
            val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
            val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
            val tvUpiId: TextView = itemView.findViewById(R.id.tvUpiId)
            val tvSourceBadge: TextView = itemView.findViewById(R.id.tvSourceBadge)
            val ivIcon: ImageView = itemView.findViewById(R.id.ivTransactionIcon)
        }
    }
}
