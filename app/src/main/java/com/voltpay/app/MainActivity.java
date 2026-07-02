package com.voltpay.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.voltpay.app.data.db.TransactionDao;
import com.voltpay.app.data.model.Transaction;
import com.voltpay.app.ui.analytics.AnalyticsActivity;
import com.voltpay.app.ui.contacts.ContactsActivity;
import com.voltpay.app.ui.history.HistoryActivity;
import com.voltpay.app.ui.payment.SendMoneyActivity;
import com.voltpay.app.ui.scan.MyQrActivity;
import com.voltpay.app.ui.scan.ScanQrActivity;
import com.voltpay.app.ui.settings.SettingsActivity;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import android.content.SharedPreferences;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private TransactionDao transactionDao;
    private SharedPreferences securePrefs;
    private RecyclerView rvRecentTransactions;
    private View emptyStateLayout;
    private TransactionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize DAO utilizing the Secure KeyStore passphrase automatically
        transactionDao = new TransactionDao(this);

        rvRecentTransactions = findViewById(R.id.rvRecentTransactions);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        
        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(this));

        // Set up Quick Actions
        View btnHeaderMyQr = findViewById(R.id.btnHeaderMyQr);
        if (btnHeaderMyQr != null) {
            btnHeaderMyQr.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, MyQrActivity.class));
            });
        }

        findViewById(R.id.btnActionRequest).setOnClickListener(v -> {
            checkUpiIdAndProceed(com.voltpay.app.ui.request.RequestActivity.class);
        });
        findViewById(R.id.btnActionHistory).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, HistoryActivity.class));
        });

        initSecurePrefs();

        View btnActionPay = findViewById(R.id.btnActionPay);
        if (btnActionPay != null) {
            btnActionPay.setOnClickListener(v -> checkUpiIdAndProceed(SendMoneyActivity.class));
        }

        View btnActionContacts = findViewById(R.id.btnActionContacts);
        if (btnActionContacts != null) {
            btnActionContacts.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ContactsActivity.class)));
        }

        View btnActionScan = findViewById(R.id.btnActionScan);
        if (btnActionScan != null) {
            btnActionScan.setOnClickListener(v -> checkUpiIdAndProceed(ScanQrActivity.class));
        }

        View btnHeaderMyQrTop = findViewById(R.id.btnHeaderMyQr);
        if (btnHeaderMyQrTop != null) {
            btnHeaderMyQrTop.setOnClickListener(v -> checkUpiIdAndProceed(MyQrActivity.class));
        }

        View btnActionRequestTop = findViewById(R.id.btnActionRequest);
        if (btnActionRequestTop != null) {
            btnActionRequestTop.setOnClickListener(v -> checkUpiIdAndProceed(com.voltpay.app.ui.request.RequestActivity.class));
        }

        View tvViewAll = findViewById(R.id.tvViewAll);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HistoryActivity.class)));
        }

        View btnActionAnalytics = findViewById(R.id.btnActionAnalytics);
        if (btnActionAnalytics != null) {
            btnActionAnalytics.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AnalyticsActivity.class)));
        }

        View btnSettings = findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
        }

        View btnActionBalance = findViewById(R.id.btnActionBalance);
        if (btnActionBalance != null) {
            btnActionBalance.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, com.voltpay.app.ui.balance.BalanceActivity.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentTransactions();
        loadUserProfile();
    }
    
    private void initSecurePrefs() {
        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            securePrefs = EncryptedSharedPreferences.create(
                    this,
                    "voltpay_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    private void checkUpiIdAndProceed(Class<?> targetActivity) {
        if (securePrefs == null) return;
        
        String savedUpi = securePrefs.getString("user_upi_id", "");
        if (savedUpi == null || savedUpi.isEmpty()) {
            showLazyUpiIdSheet(targetActivity);
        } else {
            startActivity(new Intent(this, targetActivity));
        }
    }

    private void showLazyUpiIdSheet(Class<?> targetActivity) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_upi_id, null);
        dialog.setContentView(view);
        
        EditText etUpiId = view.findViewById(R.id.etUpiId);
        Button btnSave = view.findViewById(R.id.btnSaveUpi);
        
        String phone = securePrefs.getString("user_phone_number", "");
        if (phone != null && !phone.isEmpty()) {
            etUpiId.setText(phone + "@");
            etUpiId.setSelection(etUpiId.getText().length());
        }

        btnSave.setOnClickListener(v -> {
            String upi = etUpiId.getText() != null ? etUpiId.getText().toString().trim() : "";
            if (!upi.matches("[a-zA-Z0-9._-]+@[a-zA-Z]+")) {
                etUpiId.setError("Invalid UPI ID format");
                return;
            }
            securePrefs.edit().putString("user_upi_id", upi).apply();
            dialog.dismiss();
            startActivity(new Intent(this, targetActivity));
        });
        
        dialog.show();
    }

    private void loadUserProfile() {
        if (securePrefs != null) {
            String name = securePrefs.getString("user_name", "User");
            TextView tvGreeting = findViewById(R.id.tvGreeting);
            if (tvGreeting != null) {
                tvGreeting.setText("Hi, " + name);
            }
        }
    }

    private void loadRecentTransactions() {
        // Fetch all transactions and limit to top 3 for preview
        List<Transaction> allTransactions = transactionDao.getAllTransactions();
        List<Transaction> previewList = allTransactions.size() > 3 ? allTransactions.subList(0, 3) : allTransactions;

        if (previewList.isEmpty()) {
            rvRecentTransactions.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            rvRecentTransactions.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            
            adapter = new TransactionAdapter(previewList);
            rvRecentTransactions.setAdapter(adapter);
        }
    }

    // Inner Adapter for the RecyclerView
    private static class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
        private final List<Transaction> transactions;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());

        public TransactionAdapter(List<Transaction> transactions) {
            this.transactions = transactions;
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
            holder.tvTimestamp.setText(dateFormat.format(new Date(tx.getTimestamp())));
            
            if ("CREDIT".equalsIgnoreCase(tx.getType())) {
                holder.tvAmount.setText(String.format(Locale.getDefault(), "+ ₹%.2f", tx.getAmount()));
                holder.tvAmount.setTextColor(Color.parseColor("#10B981")); // Emerald green
                holder.ivIcon.setBackgroundColor(Color.parseColor("#10B981"));
            } else {
                holder.tvAmount.setText(String.format(Locale.getDefault(), "- ₹%.2f", tx.getAmount()));
                holder.tvAmount.setTextColor(Color.parseColor("#0F172A")); // Dark Slate
                holder.ivIcon.setBackgroundColor(Color.parseColor("#0F172A"));
            }
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCounterparty, tvTimestamp, tvAmount;
            View ivIcon;
            
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCounterparty = itemView.findViewById(R.id.tvCounterparty);
                tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
                tvAmount = itemView.findViewById(R.id.tvAmount);
                ivIcon = itemView.findViewById(R.id.ivTransactionIcon);
            }
        }
    }
}
