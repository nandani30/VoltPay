package com.voltpay.app.ui.payment;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.voltpay.app.MainActivity;
import com.voltpay.app.R;

import java.util.Locale;

public class PaymentFailureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_failure);

        String errorMessage = getIntent().getStringExtra("errorMessage");
        double amount = getIntent().getDoubleExtra("amount", 0.0);
        String recipientUpiId = getIntent().getStringExtra("recipientUpiId");

        TextView tvErrorMessage = findViewById(R.id.tvErrorMessage);
        TextView tvAmount = findViewById(R.id.tvAmount);
        TextView tvCounterparty = findViewById(R.id.tvCounterparty);
        TextView tvCounterpartyUpi = findViewById(R.id.tvCounterpartyUpi);
        Button btnTryAgain = findViewById(R.id.btnTryAgain);
        Button btnHome = findViewById(R.id.btnHome);

        if (errorMessage != null && !errorMessage.isEmpty()) {
            tvErrorMessage.setText(errorMessage);
        }

        if (amount > 0) {
            tvAmount.setText(String.format(Locale.getDefault(), "₹%.2f", amount));
        } else {
            tvAmount.setVisibility(TextView.GONE);
        }

        if (recipientUpiId != null && !recipientUpiId.isEmpty() && !recipientUpiId.equals("Unknown")) {
            tvCounterparty.setText(recipientUpiId);
            tvCounterpartyUpi.setText(recipientUpiId);
        } else {
            tvCounterparty.setVisibility(TextView.GONE);
            tvCounterpartyUpi.setVisibility(TextView.GONE);
        }

        btnTryAgain.setOnClickListener(v -> {
            Intent intent = new Intent(this, SendMoneyActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (recipientUpiId != null && !recipientUpiId.equals("Unknown")) {
                intent.putExtra("EXTRA_UPI_ID", recipientUpiId);
            }
            startActivity(intent);
            finish();
        });

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
