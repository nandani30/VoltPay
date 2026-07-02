package com.voltpay.app.utils;

public class UssdSessionHolder {

    private static UssdSessionHolder instance;
    private String upiId;
    private double amount;
    private boolean expectingBalance;

    private UssdSessionHolder() {}

    public static synchronized UssdSessionHolder getInstance() {
        if (instance == null) {
            instance = new UssdSessionHolder();
        }
        return instance;
    }

    public void setPaymentSession(String upiId, double amount) {
        this.upiId = upiId;
        this.amount = amount;
        this.expectingBalance = false;
    }

    public void setBalanceSession() {
        this.expectingBalance = true;
        this.upiId = null;
        this.amount = 0;
    }

    public void clearSession() {
        this.upiId = null;
        this.amount = 0;
        this.expectingBalance = false;
    }

    public boolean isActive() {
        return (upiId != null && amount > 0) || expectingBalance;
    }

    public String getUpiId() {
        return upiId;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isExpectingBalance() {
        return expectingBalance;
    }
}
