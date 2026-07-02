package com.voltpay.app.ui.history;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

// Removed Chip import
import com.voltpay.app.R;
import com.voltpay.app.data.db.TransactionDao;
import com.voltpay.app.data.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyStateLayout;
    private TransactionDao transactionDao;
    private HistoryAdapter adapter;
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        transactionDao = new TransactionDao(this);
        
        rvHistory = findViewById(R.id.rvHistory);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        
        swipeRefresh.setColorSchemeColors(Color.parseColor("#2ED573"));
        swipeRefresh.setProgressBackgroundColorSchemeColor(Color.parseColor("#1A1A1A"));
        
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView chipAll = findViewById(R.id.chipAll);
        TextView chipSent = findViewById(R.id.chipSent);
        TextView chipReceived = findViewById(R.id.chipReceived);

        chipAll.setOnClickListener(v -> {
            updateChips(chipAll, chipSent, chipReceived);
            currentFilter = "ALL";
            loadHistory(currentFilter);
        });
        chipSent.setOnClickListener(v -> {
            updateChips(chipSent, chipAll, chipReceived);
            currentFilter = "DEBIT";
            loadHistory(currentFilter);
        });
        chipReceived.setOnClickListener(v -> {
            updateChips(chipReceived, chipAll, chipSent);
            currentFilter = "CREDIT";
            loadHistory(currentFilter);
        });

        swipeRefresh.setOnRefreshListener(() -> loadHistory(currentFilter));

        loadHistory(currentFilter);
    }

    private void updateChips(TextView selected, TextView unselected1, TextView unselected2) {
        selected.setTextColor(Color.parseColor("#FFFFFF"));
        selected.setBackgroundColor(Color.parseColor("#2ED573"));
        
        unselected1.setTextColor(Color.parseColor("#A0A0A0"));
        unselected1.setBackgroundColor(Color.parseColor("#1A1A1A"));
        
        unselected2.setTextColor(Color.parseColor("#A0A0A0"));
        unselected2.setBackgroundColor(Color.parseColor("#1A1A1A"));
    }

    private void loadHistory(String filter) {
        List<Transaction> transactions;
        if ("DEBIT".equals(filter) || "CREDIT".equals(filter)) {
            transactions = transactionDao.getByType(filter);
        } else {
            transactions = transactionDao.getAllTransactions();
        }

        swipeRefresh.setRefreshing(false);

        if (transactions.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            rvHistory.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            adapter = new HistoryAdapter(transactions, tx -> {
                Intent intent = new Intent(this, TransactionDetailActivity.class);
                intent.putExtra("transaction_id", tx.getId());
                startActivity(intent);
            });
            rvHistory.setAdapter(adapter);
        }
    }

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private final List<Transaction> transactions;
        private final OnTransactionClickListener listener;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

        public HistoryAdapter(List<Transaction> transactions, OnTransactionClickListener listener) {
            this.transactions = transactions;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction_preview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Transaction tx = transactions.get(position);
            
            holder.tvCounterparty.setText(tx.getCounterparty() != null ? tx.getCounterparty() : "Unknown");
            
            // Format UPI ID based on the counterparty assuming counterparty contains UPI ID or it's separate. 
            // In VoltPay, often counterparty IS the upi_id if name isn't resolved.
            // But let's just put the raw text or derive it. For now, if upi_id is passed, we show it. 
            // Transaction model doesn't have a separate upiId field except counterparty, so we'll show counterparty in both or hide one if they match.
            // Wait, the prompt says "Add UPI ID as second line below counterparty name". If we only have counterparty, we'll put it there.
            holder.tvUpiId.setText(tx.getCounterparty()); 
            
            holder.tvTimestamp.setText(dateFormat.format(new Date(tx.getTimestamp())));
            
            if ("CREDIT".equals(tx.getType())) {
                holder.tvAmount.setText("+ ₹" + String.format(Locale.getDefault(), "%.2f", tx.getAmount()));
                holder.tvAmount.setTextColor(Color.parseColor("#4ADE80")); // Emerald green
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_revert);
                holder.ivIcon.setRotation(90);
                holder.ivIcon.setColorFilter(Color.parseColor("#4ADE80"));
            } else {
                holder.tvAmount.setText("- ₹" + String.format(Locale.getDefault(), "%.2f", tx.getAmount()));
                holder.tvAmount.setTextColor(Color.parseColor("#FF4444")); // Slate dark
                holder.ivIcon.setImageResource(android.R.drawable.ic_menu_send);
                holder.ivIcon.setRotation(0);
                holder.ivIcon.setColorFilter(Color.parseColor("#FF4444"));
            }

            // Source badge
            holder.tvSourceBadge.setText(tx.getSource() != null ? tx.getSource().toUpperCase() : "UNKNOWN");
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(16f);
            if ("AUTO".equalsIgnoreCase(tx.getSource())) {
                bg.setColor(Color.parseColor("#2196F3"));
            } else if ("MANUAL".equalsIgnoreCase(tx.getSource())) {
                bg.setColor(Color.parseColor("#9E9E9E"));
            } else {
                bg.setColor(Color.parseColor("#9E9E9E"));
            }
            holder.tvSourceBadge.setBackground(bg);

            holder.itemView.setOnClickListener(v -> listener.onTransactionClick(tx));
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCounterparty, tvAmount, tvTimestamp, tvUpiId, tvSourceBadge;
            ImageView ivIcon;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCounterparty = itemView.findViewById(R.id.tvCounterparty);
                tvUpiId = itemView.findViewById(R.id.tvUpiId);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
                tvSourceBadge = itemView.findViewById(R.id.tvSourceBadge);
                ivIcon = itemView.findViewById(R.id.ivTransactionIcon);
            }
        }
    }
}
