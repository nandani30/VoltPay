package com.voltpay.app.ui.payment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.voltpay.app.MainActivity;
import com.voltpay.app.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PaymentSuccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_success);

        double amount = getIntent().getDoubleExtra("amount", 0.0);
        String recipientName = getIntent().getStringExtra("recipientName");
        String recipientUpiId = getIntent().getStringExtra("recipientUpiId");
        String upiRef = getIntent().getStringExtra("upiRef");
        long timestamp = getIntent().getLongExtra("timestamp", System.currentTimeMillis());
        String source = getIntent().getStringExtra("source"); 
        
        if (source == null) {
            source = "MANUAL";
        }
        
        boolean isAuto = getIntent().getBooleanExtra("isAuto", false);

        TextView tvAmount = findViewById(R.id.tvAmount);
        TextView tvCounterparty = findViewById(R.id.tvCounterparty);
        TextView tvCounterpartyUpi = findViewById(R.id.tvCounterpartyUpi);
        TextView tvRefNumber = findViewById(R.id.tvRefNumber);
        TextView tvSourceBadge = findViewById(R.id.tvSourceBadge);
        TextView tvDateTime = findViewById(R.id.tvDateTime);
        View btnCopyRef = findViewById(R.id.btnCopyRef);
        Button btnDone = findViewById(R.id.btnDone);

        tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", amount));
        
        if (recipientName != null && !recipientName.equals(recipientUpiId) && !recipientName.equals("Unknown")) {
            tvCounterparty.setText(recipientName);
            tvCounterpartyUpi.setText(recipientUpiId);
        } else {
            tvCounterparty.setText(recipientUpiId != null ? recipientUpiId : "Unknown");
            tvCounterpartyUpi.setText("");
        }

        tvRefNumber.setText(upiRef != null ? upiRef : "N/A");
        tvSourceBadge.setText(isAuto ? "AUTO" : "MANUAL");
        
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM hh:mm a", Locale.getDefault());
        tvDateTime.setText(sdf.format(new Date(timestamp)));

        btnCopyRef.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("UPI Reference", upiRef != null ? upiRef : "N/A");
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Reference copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        btnDone.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
