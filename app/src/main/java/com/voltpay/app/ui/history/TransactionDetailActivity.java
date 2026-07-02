package com.voltpay.app.ui.history;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.voltpay.app.R;
import com.voltpay.app.data.db.TransactionDao;
import com.voltpay.app.data.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        long txId = getIntent().getLongExtra("transaction_id", -1);
        if (txId == -1) {
            Toast.makeText(this, "Invalid transaction", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TransactionDao dao = new TransactionDao(this);
        Transaction tx = dao.getById(txId);
        if (tx == null) {
            Toast.makeText(this, "Transaction not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView tvAmount = findViewById(R.id.tvAmount);
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvToFromLabel = findViewById(R.id.tvToFromLabel);
        TextView tvCounterpartyName = findViewById(R.id.tvCounterpartyName);
        TextView tvCounterpartyUpi = findViewById(R.id.tvCounterpartyUpi);
        TextView tvUpiRef = findViewById(R.id.tvUpiRef);
        TextView tvDateTime = findViewById(R.id.tvDateTime);
        TextView tvSource = findViewById(R.id.tvSource);
        ImageView btnCopyRef = findViewById(R.id.btnCopyRef);

        boolean isCredit = "CREDIT".equals(tx.getType());

        if (isCredit) {
            tvAmount.setText(String.format(Locale.getDefault(), "+ ₹%.2f", tx.getAmount()));
            tvAmount.setTextColor(android.graphics.Color.parseColor("#10B981"));
            tvToFromLabel.setText("From");
        } else {
            tvAmount.setText(String.format(Locale.getDefault(), "- ₹%.2f", tx.getAmount()));
            tvAmount.setTextColor(android.graphics.Color.parseColor("#F44336"));
            tvToFromLabel.setText("To");
        }

        tvCounterpartyName.setText(tx.getCounterparty() != null ? tx.getCounterparty() : "Unknown");
        tvCounterpartyUpi.setText(tx.getCounterparty() != null ? tx.getCounterparty() : "Unknown");
        
        tvUpiRef.setText(tx.getUpiRef() != null ? tx.getUpiRef() : "N/A");

        SimpleDateFormat fullDateFormat = new SimpleDateFormat("EEEE, dd MMMM yyyy, hh:mm a", Locale.getDefault());
        tvDateTime.setText(fullDateFormat.format(new Date(tx.getTimestamp())));

        if ("AUTO".equalsIgnoreCase(tx.getSource())) {
            tvSource.setText("Processed automatically");
        } else {
            tvSource.setText("Confirmed manually");
        }

        btnCopyRef.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("UPI Reference", tx.getUpiRef() != null ? tx.getUpiRef() : "N/A");
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
        });
    }
}
