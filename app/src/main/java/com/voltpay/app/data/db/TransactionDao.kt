package com.voltpay.app.data.db

import android.content.ContentValues
import android.content.Context
import com.voltpay.app.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale

class TransactionDao(context: Context) {
    private val dbHelper: DatabaseHelper = DatabaseHelper.getInstance(context)

    fun insertTransaction(tx: Transaction): Long {
        val db = dbHelper.secureWritableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_TX_AMOUNT, tx.amount)
            put(DatabaseHelper.COL_TX_TYPE, tx.type)
            put(DatabaseHelper.COL_TX_UPI_REF, tx.upiRef)
            put(DatabaseHelper.COL_TX_COUNTERPARTY, tx.counterparty)
            put(DatabaseHelper.COL_TX_TIMESTAMP, tx.timestamp)
            put(DatabaseHelper.COL_TX_RAW_SMS, tx.rawSms)
            put(DatabaseHelper.COL_TX_SOURCE, tx.source)
        }
        val id = db.insert(DatabaseHelper.TABLE_TRANSACTIONS, null, values)
        db.close()
        return id
    }

    fun getAllTransactions(): List<Transaction> {
        val transactions = ArrayList<Transaction>()
        val db = dbHelper.secureReadableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_TRANSACTIONS, null, null, null, null, null, DatabaseHelper.COL_TX_TIMESTAMP + " DESC")
        
        if (cursor.moveToFirst()) {
            do {
                val tx = Transaction()
                tx.id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_ID))
                tx.amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_AMOUNT))
                tx.type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TYPE))
                tx.upiRef = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_UPI_REF))
                tx.counterparty = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_COUNTERPARTY))
                tx.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TIMESTAMP))
                tx.rawSms = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_RAW_SMS))
                tx.source = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_SOURCE))
                transactions.add(tx)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return transactions
    }

    fun getByType(type: String): List<Transaction> {
        val transactions = ArrayList<Transaction>()
        val db = dbHelper.secureReadableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_TRANSACTIONS, null, DatabaseHelper.COL_TX_TYPE + " = ?", arrayOf(type), null, null, DatabaseHelper.COL_TX_TIMESTAMP + " DESC")
        
        if (cursor.moveToFirst()) {
            do {
                val tx = Transaction()
                tx.id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_ID))
                tx.amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_AMOUNT))
                tx.type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TYPE))
                tx.upiRef = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_UPI_REF))
                tx.counterparty = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_COUNTERPARTY))
                tx.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TIMESTAMP))
                tx.rawSms = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_RAW_SMS))
                tx.source = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_SOURCE))
                transactions.add(tx)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return transactions
    }

    fun getByDateRange(startTime: Long, endTime: Long): List<Transaction> {
        val transactions = ArrayList<Transaction>()
        val db = dbHelper.secureReadableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_TRANSACTIONS, null, DatabaseHelper.COL_TX_TIMESTAMP + " >= ? AND " + DatabaseHelper.COL_TX_TIMESTAMP + " <= ?", arrayOf(startTime.toString(), endTime.toString()), null, null, DatabaseHelper.COL_TX_TIMESTAMP + " DESC")
        
        if (cursor.moveToFirst()) {
            do {
                val tx = Transaction()
                tx.id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_ID))
                tx.amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_AMOUNT))
                tx.type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TYPE))
                tx.upiRef = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_UPI_REF))
                tx.counterparty = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_COUNTERPARTY))
                tx.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TIMESTAMP))
                tx.rawSms = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_RAW_SMS))
                tx.source = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_SOURCE))
                transactions.add(tx)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return transactions
    }

    fun getById(id: Long): Transaction? {
        val db = dbHelper.secureReadableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_TRANSACTIONS, null, DatabaseHelper.COL_TX_ID + " = ?", arrayOf(id.toString()), null, null, null)
        var tx: Transaction? = null
        if (cursor.moveToFirst()) {
            tx = Transaction()
            tx.id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_ID))
            tx.amount = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_AMOUNT))
            tx.type = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TYPE))
            tx.upiRef = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_UPI_REF))
            tx.counterparty = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_COUNTERPARTY))
            tx.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TIMESTAMP))
            tx.rawSms = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_RAW_SMS))
            tx.source = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_SOURCE))
        }
        cursor.close()
        db.close()
        return tx
    }

    fun getTotalAmountByType(type: String, startTime: Long, endTime: Long): Double {
        val db = dbHelper.secureReadableDatabase
        val cursor = db.rawQuery("SELECT SUM(" + DatabaseHelper.COL_TX_AMOUNT + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS + 
            " WHERE " + DatabaseHelper.COL_TX_TYPE + " = ? AND " + DatabaseHelper.COL_TX_TIMESTAMP + " >= ? AND " + DatabaseHelper.COL_TX_TIMESTAMP + " <= ?", 
            arrayOf(type, startTime.toString(), endTime.toString()))
        var total = 0.0
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0)
        }
        cursor.close()
        db.close()
        return total
    }

    fun getDailyTotals(startTime: Long, endTime: Long): LinkedHashMap<String, Double> {
        val totals = LinkedHashMap<String, Double>()
        val cal = Calendar.getInstance()
        cal.timeInMillis = startTime
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        
        while (cal.timeInMillis <= endTime) {
            totals[sdf.format(cal.time)] = 0.0
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val txs = getByDateRange(startTime, endTime)
        for (tx in txs) {
            if ("DEBIT" == tx.type) {
                val dayStr = sdf.format(Date(tx.timestamp))
                if (totals.containsKey(dayStr)) {
                    totals[dayStr] = totals[dayStr]!! + tx.amount
                }
            }
        }
        return totals
    }

    fun getMonthlyTotals(numberOfMonths: Int): LinkedHashMap<String, Double> {
        val totals = LinkedHashMap<String, Double>()
        
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -(numberOfMonths - 1))
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()
        
        val sdf = SimpleDateFormat("MMM", Locale.getDefault())
        
        for (i in 0 until numberOfMonths) {
            totals[sdf.format(cal.time)] = 0.0
            cal.add(Calendar.MONTH, 1)
        }
        
        val txs = getByDateRange(startTime, endTime)
        for (tx in txs) {
            if ("DEBIT" == tx.type) {
                val monthStr = sdf.format(Date(tx.timestamp))
                if (totals.containsKey(monthStr)) {
                    totals[monthStr] = totals[monthStr]!! + tx.amount
                }
            }
        }
        return totals
    }
}
