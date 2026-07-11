package com.voltpay.app.utils

object BalanceHolder {
    var isExpectingBalance: Boolean = false
    private var balance: Double = 0.0

    fun setBalance(amount: Double) {
        this.balance = amount
    }

    fun getAndClearBalance(): Double {
        val currentBalance = this.balance
        this.isExpectingBalance = false
        this.balance = 0.0
        return currentBalance
    }
}
