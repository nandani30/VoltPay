package com.voltpay.app.ui.payment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.voltpay.app.R;
import com.voltpay.app.service.VoltPayAccessibilityService;
import com.voltpay.app.ussd.UssdEncoder;
import com.voltpay.app.utils.UssdSessionHolder;

public class PaymentProcessingActivity extends AppCompatActivity {

    private static final long TIMEOUT_MS = 90_000;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private boolean resultReceived = false;

    private final BroadcastReceiver resultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (resultReceived) return;
            resultReceived = true;
            timeoutHandler.removeCallbacksAndMessages(null);
            // PaymentSuccessActivity or PaymentFailureActivity is launched
            // by the accessibility service directly. Just finish here.
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_processing);

        String recipientUpi = getIntent().getStringExtra("EXTRA_UPI_ID");
        double originalAmount = getIntent().getDoubleExtra("EXTRA_AMOUNT", 0.0);

        TextView tvStatus = findViewById(R.id.tvStatus);
        if (tvStatus != null) tvStatus.setText("Connecting to bank...");

        // Cancel button
        Button btnCancel = findViewById(R.id.btnCancelPayment);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> cancelPayment(originalAmount, recipientUpi));
        }

        // Register for accessibility service broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(VoltPayAccessibilityService.ACTION_USSD_SUCCESS);
        filter.addAction(VoltPayAccessibilityService.ACTION_USSD_ERROR);
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(resultReceiver, filter);

        UssdSessionHolder.getInstance().setPaymentSession(recipientUpi, originalAmount);

        // 90 second timeout
        timeoutHandler.postDelayed(() -> {
            if (!resultReceived) {
                cancelPayment(originalAmount, recipientUpi);
            }
        }, TIMEOUT_MS);

        // Dial after short delay so layout renders first
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            dialUssd(recipientUpi, originalAmount);
        }, 1500);
    }

    private void cancelPayment(double amount, String upiId) {
        resultReceived = true;
        timeoutHandler.removeCallbacksAndMessages(null);
        UssdSessionHolder.getInstance().clearSession();
        Intent failureIntent = new Intent(this, PaymentFailureActivity.class);
        failureIntent.putExtra("errorMessage", "Payment was cancelled or timed out.");
        failureIntent.putExtra("amount", amount);
        failureIntent.putExtra("recipientUpiId", upiId);
        startActivity(failureIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeoutHandler.removeCallbacksAndMessages(null);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(resultReceiver);
    }

    private void dialUssd(String upiId, double amount) {
        String ussdString;
        
        // Basic detection if it's a phone number or UPI ID for the single-stage string
        if (upiId != null && upiId.matches("\\d{10}")) {
            ussdString = UssdEncoder.buildSingleStageSendMoneyPhone(upiId, amount);
        } else {
            ussdString = UssdEncoder.buildSingleStageSendMoneyUpi(upiId, amount);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            UssdEncoder.executeUssdRequest(this, ussdString, new UssdEncoder.UssdCallback() {
                @Override
                public void onResponse(String text) {
                    // Usually sendUssdRequest doesn't handle multi-stage dialogs natively, 
                    // it fires the intent and relies on accessibility or native dialer UI for conversational steps.
                    // But if it fails immediately, we catch it here.
                    String lower = text.toLowerCase();
                    if (lower.contains("failed") || lower.contains("error")) {
                        handleFailure("USSD Failed: " + text, amount, upiId);
                    }
                }

                @Override
                public void onFailure(String error) {
                    Toast.makeText(PaymentProcessingActivity.this, "Single-stage failed, falling back to manual dialer", Toast.LENGTH_SHORT).show();
                    fallbackToDialer(UssdEncoder.buildSendMoneyInitialString());
                }
            });
        } else {
            fallbackToDialer(ussdString);
        }
    }

    private void fallbackToDialer(String ussdString) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + UssdEncoder.encodeUssd(ussdString)));
        try {
            startActivity(intent);
        } catch (SecurityException e) {
            Toast.makeText(this, "Phone permission denied", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void handleFailure(String errorMsg, double amount, String recipientUpiId) {
        resultReceived = true;
        UssdSessionHolder.getInstance().clearSession();
        Intent failureIntent = new Intent(this, PaymentFailureActivity.class);
        failureIntent.putExtra("errorMessage", errorMsg);
        failureIntent.putExtra("amount", amount);
        failureIntent.putExtra("recipientUpiId", recipientUpiId);
        startActivity(failureIntent);
        finish();
    }
}
