package com.voltpay.app.ui.payment;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.voltpay.app.R;
import android.content.Intent;
import com.voltpay.app.ui.contacts.ContactsActivity;

public class SendMoneyActivity extends AppCompatActivity {

    private EditText etAmount;
    private EditText etUpiId;
    private Button btnPay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // SECURITY: FLAG_SECURE prevents screen recording and screenshots
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        
        setContentView(R.layout.activity_send_money);

        etAmount = findViewById(R.id.etAmount);
        etUpiId = findViewById(R.id.etUpiId);
        btnPay = findViewById(R.id.btnPay);

        if (btnPay != null) {
            btnPay.setOnClickListener(v -> handlePayment());
        }
        
        android.widget.TextView tvSelectContact = findViewById(R.id.tvSelectContact);
        if (tvSelectContact != null) {
            tvSelectContact.setOnClickListener(v -> {
                Intent intent = new Intent(this, ContactsActivity.class);
                startActivity(intent);
                finish(); // Close SendMoneyActivity so back button from Contacts returns to Dashboard
            });
        }
    }

    private void handlePayment() {
        if (etAmount == null || etUpiId == null) return;
        
        String amountStr = etAmount.getText().toString().trim();
        String upiId = etUpiId.getText().toString().trim();

        if (amountStr.isEmpty() || upiId.isEmpty()) {
            Toast.makeText(this, "Please enter all fields correctly", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = 0.0;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidUpiId(upiId) && !isValidMobileNumber(upiId)) {
            Toast.makeText(this,
                "Please enter a valid UPI ID (e.g. name@bank) or 10-digit mobile number",
                Toast.LENGTH_LONG).show();
            return;
        }

        if (amount <= 0 || amount > 100000) {
            Toast.makeText(this,
                "Amount must be between ₹1 and ₹1,00,000",
                Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PaymentProcessingActivity.class);
        intent.putExtra("EXTRA_UPI_ID", upiId);
        intent.putExtra("EXTRA_AMOUNT", amount);
        startActivity(intent);
        finish();
    }

    private boolean isValidUpiId(String upiId) {
        // Standard UPI ID format: localpart@psp
        // localpart: alphanumeric, dots, hyphens, underscores (3-256 chars)
        // psp: alphabetic (2-64 chars)
        return upiId != null && upiId.matches("^[a-zA-Z0-9.\\-_]{3,256}@[a-zA-Z]{2,64}$");
    }

    private boolean isValidMobileNumber(String input) {
        return input != null && input.matches("^[6-9]\\d{9}$"); // Indian mobile
    }
}
