package com.voltpay.app.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.voltpay.app.data.db.ContactDao;
import com.voltpay.app.data.db.TransactionDao;
import com.voltpay.app.data.model.Transaction;
import com.voltpay.app.utils.UssdSessionHolder;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoltPayAccessibilityService extends AccessibilityService {

    private static final String TAG = "VoltPayA11y";
    
    public static final String ACTION_USSD_UPDATE = "com.voltpay.app.USSD_UPDATE";
    public static final String ACTION_USSD_SUCCESS = "com.voltpay.app.USSD_SUCCESS";
    public static final String ACTION_USSD_ERROR = "com.voltpay.app.USSD_ERROR";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        if (!packageName.equals("com.android.phone") && 
            !packageName.equals("android") && 
            !packageName.equals("com.android.systemui")) {
            return;
        }

        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
            event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                processUssdDialog(rootNode);
            } else {
                // If the window is gone and we were expecting a session, it might have been cancelled
                // But we don't aggressively clear it here to avoid race conditions.
            }
        }
    }

    private void processUssdDialog(AccessibilityNodeInfo node) {
        StringBuilder dialogText = new StringBuilder();
        extractText(node, dialogText);
        String raw = dialogText.toString();
        String text = raw.toLowerCase().trim();

        UssdSessionHolder session = UssdSessionHolder.getInstance();

        // --- SUCCESS DETECTION ---
        if (text.contains("success") || text.contains("sent successfully") ||
            text.contains("paid successfully") || text.contains("payment successful") ||
            text.contains("money sent") || text.contains("transfer successful") ||
            text.contains("approved") || text.contains("debited")) {
            parseAndSaveTransaction(raw);
            clickButtonByText(node, "OK", "Dismiss", "Cancel", "Close", "Done");
            session.clearSession();
        }

        // --- FAILURE DETECTION ---
        else if (text.contains("failed") || text.contains("failure") ||
                 text.contains("error") || text.contains("invalid") ||
                 text.contains("declined") || text.contains("insufficient") ||
                 text.contains("not found") || text.contains("try again") ||
                 text.contains("unavailable") || text.contains("rejected")) {
            broadcastError("Transaction Failed: " + raw);
            clickButtonByText(node, "OK", "Dismiss", "Cancel", "Close", "Done");
            launchFailureScreen(raw, session.getAmount(), session.getUpiId());
            session.clearSession();
        }

        // --- BALANCE DETECTION ---
        else if ((text.contains("balance") || text.contains("avl bal") ||
                  text.contains("available balance") || text.contains("bal is")) &&
                 session.isExpectingBalance()) {
            parseAndSaveBalance(raw);
            clickButtonByText(node, "OK", "Dismiss", "Cancel", "Close", "Done");
            session.clearSession();
        }

        // --- FALLBACK MENU NAVIGATION (non-secret steps only) ---
        else if (session.isActive()) {

            // Welcome / main menu
            if (text.contains("welcome") || text.contains("*99#") ||
                text.contains("select") || text.contains("main menu") ||
                text.contains("nuup") || text.contains("send money") && text.contains("check balance")) {
                fillEditText(node, session.isExpectingBalance() ? "3" : "1");
                clickButtonByText(node, "Send", "OK", "Reply", "Submit");
            }

            // Sub-menu: choose transfer method (mobile / UPI ID / MMID)
            else if ((text.contains("mobile no") || text.contains("mobile number") ||
                      text.contains("upi id") || text.contains("mmid")) &&
                     (text.contains("1.") || text.contains("1)"))) {
                // Always choose UPI ID option (usually 3, sometimes 2)
                String choice = text.contains("3") && text.contains("upi") ? "3" : "2";
                fillEditText(node, choice);
                clickButtonByText(node, "Send", "OK", "Reply", "Submit");
            }

            // UPI ID entry — DO fill (not a secret)
            else if (text.contains("upi id") || text.contains("vpa") ||
                     text.contains("pay to") || text.contains("beneficiary id")) {
                if (session.getUpiId() != null && !session.getUpiId().isEmpty()) {
                    fillEditText(node, session.getUpiId());
                    clickButtonByText(node, "Send", "OK", "Reply", "Submit");
                }
            }

            // Amount entry — DO fill (not a secret)
            else if (text.contains("amount") || text.contains("enter amt")) {
                if (session.getAmount() > 0) {
                    // Send as integer paise-safe string; omit paise if whole number
                    String amountStr = session.getAmount() == Math.floor(session.getAmount())
                        ? String.valueOf((int) session.getAmount())
                        : String.valueOf(session.getAmount());
                    fillEditText(node, amountStr);
                    clickButtonByText(node, "Send", "OK", "Reply", "Submit");
                }
            }

            // Remark/reference — skip with generic value
            else if (text.contains("remark") || text.contains("reference") ||
                     text.contains("narration") || text.contains("note")) {
                fillEditText(node, "Payment");
                clickButtonByText(node, "Send", "OK", "Reply", "Submit");
            }

            // PIN screen — NEVER touch. User types their own PIN.
            else if (text.contains("pin") || text.contains("mpin") ||
                     text.contains("upi pin") || text.contains("password")) {
                Log.d(TAG, "PIN screen reached — waiting for user to type their PIN.");
                // Do nothing. The user types their PIN into the carrier's own dialog.
            }
        }
    }

    private void parseAndSaveTransaction(String rawText) {
        Pattern amountPattern = Pattern.compile("(?:rs\\.?\\s*|inr\\s*)([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
        Pattern upiPattern = Pattern.compile("([a-zA-Z0-9.\\-_]+@[a-zA-Z]+)", Pattern.CASE_INSENSITIVE);
        Pattern namePattern = Pattern.compile("to\\s+(.+?)\\s+successful", Pattern.CASE_INSENSITIVE);
        Pattern refPattern = Pattern.compile("(?:ref|txn\\s*id|reference)\\s*[:\\-]?\\s*([0-9]{9,15})", Pattern.CASE_INSENSITIVE);

        Matcher amtMatcher = amountPattern.matcher(rawText);
        Matcher upiMatcher = upiPattern.matcher(rawText);
        Matcher nameMatcher = namePattern.matcher(rawText);
        Matcher refMatcher = refPattern.matcher(rawText);

        double amount = 0.0;
        String upiId = "Unknown";
        String recipientName = "Unknown";
        String refNo = "N/A";

        if (amtMatcher.find()) {
            try { amount = Double.parseDouble(amtMatcher.group(1)); } catch (Exception e) {}
        }
        
        if (nameMatcher.find()) {
            recipientName = nameMatcher.group(1).trim(); 
            upiId = recipientName;
        } else if (upiMatcher.find()) {
            upiId = upiMatcher.group(1);
            recipientName = upiId;
        }
        
        if (refMatcher.find()) {
            refNo = refMatcher.group(1);
        }

        if (amount > 0) {
            TransactionDao dao = new TransactionDao(this);
            Transaction t = new Transaction();
            t.setAmount(amount);
            t.setCounterparty(upiId);
            t.setUpiRef(refNo);
            t.setType("DEBIT");
            long timestamp = System.currentTimeMillis();
            t.setTimestamp(timestamp);
            t.setSource("AUTO");
            t.setRawSms(rawText);
            
            try {
                dao.insertTransaction(t);
                
                ContactDao contactDao = new ContactDao(this);
                if (contactDao.getContactByUpiId(upiId) == null) {
                    Intent promptIntent = new Intent(this, com.voltpay.app.ui.contacts.SaveContactPromptActivity.class);
                    promptIntent.putExtra("EXTRA_UPI_ID", upiId);
                    promptIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(promptIntent);
                } else {
                    contactDao.updateLastPaidAt(upiId, System.currentTimeMillis());
                }

                Intent successBroadcast = new Intent(ACTION_USSD_SUCCESS);
                LocalBroadcastManager.getInstance(this).sendBroadcast(successBroadcast);

                launchSuccessScreen(amount, recipientName, upiId, refNo, timestamp);

            } catch (Exception e) {
                Log.e(TAG, "Failed to save parsed USSD transaction", e);
            }
        }
    }

    private void parseAndSaveBalance(String rawText) {
        Pattern amountPattern = Pattern.compile("(?:rs\\.?\\s*|inr\\s*|is\\s*)([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = amountPattern.matcher(rawText);
        
        if (matcher.find()) {
            try {
                double amount = Double.parseDouble(matcher.group(1));
                com.voltpay.app.utils.BalanceHolder.getInstance().setBalance(amount);
                
                Intent intent = new Intent(com.voltpay.app.ui.balance.BalanceActivity.ACTION_BALANCE_RECEIVED);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                
                Log.d(TAG, "Parsed Balance: " + amount);
            } catch (Exception e) {}
        }
    }

    private void launchSuccessScreen(double amount, String recipientName, String recipientUpiId, String upiRef, long timestamp) {
        Intent successIntent = new Intent();
        successIntent.setClassName(this, "com.voltpay.app.ui.payment.PaymentSuccessActivity");
        successIntent.putExtra("amount", amount);
        successIntent.putExtra("recipientName", recipientName);
        successIntent.putExtra("recipientUpiId", recipientUpiId);
        successIntent.putExtra("upiRef", upiRef);
        successIntent.putExtra("timestamp", timestamp);
        successIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(successIntent);
    }

    private void launchFailureScreen(String errorMessage, double amount, String recipientUpiId) {
        Intent failureIntent = new Intent();
        failureIntent.setClassName(this, "com.voltpay.app.ui.payment.PaymentFailureActivity");
        failureIntent.putExtra("errorMessage", errorMessage);
        failureIntent.putExtra("amount", amount);
        failureIntent.putExtra("recipientUpiId", recipientUpiId);
        failureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(failureIntent);
    }

    private void extractText(AccessibilityNodeInfo node, StringBuilder sb) {
        if (node == null) return;
        if (node.getText() != null) {
            sb.append(node.getText().toString()).append(" ");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            extractText(node.getChild(i), sb);
        }
    }

    private boolean clickButtonByText(AccessibilityNodeInfo rootNode, String... buttonTexts) {
        for (String text : buttonTexts) {
            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
            for (AccessibilityNodeInfo node : nodes) {
                if (node.isClickable()) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean fillEditText(AccessibilityNodeInfo rootNode, CharSequence textToFill) {
        if (textToFill == null) return false;
        AccessibilityNodeInfo editTextNode = findEditText(rootNode);
        if (editTextNode != null) {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToFill);
            editTextNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            return true;
        }
        return false;
    }

    private AccessibilityNodeInfo findEditText(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if ("android.widget.EditText".equals(node.getClassName())) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = findEditText(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    private void broadcastError(String message) {
        Intent intent = new Intent(ACTION_USSD_ERROR);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "VoltPay Accessibility Service Interrupted");
        UssdSessionHolder.getInstance().clearSession();
    }
}
