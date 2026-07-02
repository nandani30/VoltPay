package com.voltpay.app.utils;

public class BalanceHolder {
    private static BalanceHolder instance;
    private boolean expectingBalance = false;
    private double balance = 0.0;

    private BalanceHolder() {}

    public static synchronized BalanceHolder getInstance() {
        if (instance == null) {
            instance = new BalanceHolder();
        }
        return instance;
    }

    public void setExpectingBalance(boolean expectingBalance) {
        this.expectingBalance = expectingBalance;
    }

    public boolean isExpectingBalance() {
        return expectingBalance;
    }

    public void setBalance(double amount) {
        this.balance = amount;
    }

    public double getAndClearBalance() {
        double currentBalance = this.balance;
        this.expectingBalance = false;
        this.balance = 0.0;
        return currentBalance;
    }
}
