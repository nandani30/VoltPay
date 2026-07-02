package com.voltpay.app.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import com.voltpay.app.data.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public class TransactionDao {
    private final DatabaseHelper dbHelper;

    public TransactionDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public long insertTransaction(Transaction tx) {
        SQLiteDatabase db = dbHelper.getSecureWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_TX_AMOUNT, tx.getAmount());
        values.put(DatabaseHelper.COL_TX_TYPE, tx.getType());
        values.put(DatabaseHelper.COL_TX_UPI_REF, tx.getUpiRef());
        values.put(DatabaseHelper.COL_TX_COUNTERPARTY, tx.getCounterparty());
        values.put(DatabaseHelper.COL_TX_TIMESTAMP, tx.getTimestamp());
        values.put(DatabaseHelper.COL_TX_RAW_SMS, tx.getRawSms());
        values.put(DatabaseHelper.COL_TX_SOURCE, tx.getSource());
        
        long id = db.insert(DatabaseHelper.TABLE_TRANSACTIONS, null, values);
        db.close();
        return id;
    }

    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getSecureReadableDatabase();
        // Order by timestamp DESC (newest first)
        Cursor cursor = db.query(DatabaseHelper.TABLE_TRANSACTIONS, null, null, null, null, null, DatabaseHelper.COL_TX_TIMESTAMP + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                Transaction tx = new Transaction();
                tx.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_ID)));
                tx.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_AMOUNT)));
                tx.setType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TYPE)));
                tx.setUpiRef(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_UPI_REF)));
                tx.setCounterparty(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_COUNTERPARTY)));
                tx.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TIMESTAMP)));
                tx.setRawSms(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_RAW_SMS)));
                tx.setSource(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_SOURCE)));
                transactions.add(tx);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactions;
    }

    public List<Transaction> getByType(String type) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getSecureReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_TRANSACTIONS, null, DatabaseHelper.COL_TX_TYPE + " = ?", new String[]{type}, null, null, DatabaseHelper.COL_TX_TIMESTAMP + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                Transaction tx = new Transaction();
                tx.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_ID)));
                tx.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_AMOUNT)));
                tx.setType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TYPE)));
                tx.setUpiRef(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_UPI_REF)));
                tx.setCounterparty(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_COUNTERPARTY)));
                tx.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TIMESTAMP)));
                tx.setRawSms(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_RAW_SMS)));
                tx.setSource(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_SOURCE)));
                transactions.add(tx);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactions;
    }

    public List<Transaction> getByDateRange(long startTime, long endTime) {
        List<Transaction> transactions = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getSecureReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_TRANSACTIONS, null, DatabaseHelper.COL_TX_TIMESTAMP + " >= ? AND " + DatabaseHelper.COL_TX_TIMESTAMP + " <= ?", new String[]{String.valueOf(startTime), String.valueOf(endTime)}, null, null, DatabaseHelper.COL_TX_TIMESTAMP + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                Transaction tx = new Transaction();
                tx.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_ID)));
                tx.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_AMOUNT)));
                tx.setType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TYPE)));
                tx.setUpiRef(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_UPI_REF)));
                tx.setCounterparty(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_COUNTERPARTY)));
                tx.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TIMESTAMP)));
                tx.setRawSms(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_RAW_SMS)));
                tx.setSource(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_SOURCE)));
                transactions.add(tx);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return transactions;
    }

    public Transaction getById(long id) {
        SQLiteDatabase db = dbHelper.getSecureReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_TRANSACTIONS, null, DatabaseHelper.COL_TX_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        Transaction tx = null;
        if (cursor.moveToFirst()) {
            tx = new Transaction();
            tx.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_ID)));
            tx.setAmount(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_AMOUNT)));
            tx.setType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TYPE)));
            tx.setUpiRef(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_UPI_REF)));
            tx.setCounterparty(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_COUNTERPARTY)));
            tx.setTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_TIMESTAMP)));
            tx.setRawSms(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_RAW_SMS)));
            tx.setSource(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TX_SOURCE)));
        }
        cursor.close();
        db.close();
        return tx;
    }

    public double getTotalAmountByType(String type, long startTime, long endTime) {
        SQLiteDatabase db = dbHelper.getSecureReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + DatabaseHelper.COL_TX_AMOUNT + ") FROM " + DatabaseHelper.TABLE_TRANSACTIONS + 
            " WHERE " + DatabaseHelper.COL_TX_TYPE + " = ? AND " + DatabaseHelper.COL_TX_TIMESTAMP + " >= ? AND " + DatabaseHelper.COL_TX_TIMESTAMP + " <= ?", 
            new String[]{type, String.valueOf(startTime), String.valueOf(endTime)});
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return total;
    }

    public java.util.LinkedHashMap<String, Double> getDailyTotals(long startTime, long endTime) {
        java.util.LinkedHashMap<String, Double> totals = new java.util.LinkedHashMap<>();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(startTime);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault());
        
        while (cal.getTimeInMillis() <= endTime) {
            totals.put(sdf.format(cal.getTime()), 0.0);
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }
        
        List<Transaction> txs = getByDateRange(startTime, endTime);
        for (Transaction tx : txs) {
            if ("DEBIT".equals(tx.getType())) {
                String dayStr = sdf.format(new java.util.Date(tx.getTimestamp()));
                if (totals.containsKey(dayStr)) {
                    totals.put(dayStr, totals.get(dayStr) + tx.getAmount());
                }
            }
        }
        return totals;
    }

    public java.util.LinkedHashMap<String, Double> getMonthlyTotals(int numberOfMonths) {
        java.util.LinkedHashMap<String, Double> totals = new java.util.LinkedHashMap<>();
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.MONTH, -(numberOfMonths - 1));
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1);
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        
        long startTime = cal.getTimeInMillis();
        long endTime = System.currentTimeMillis();
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault());
        
        for (int i = 0; i < numberOfMonths; i++) {
            totals.put(sdf.format(cal.getTime()), 0.0);
            cal.add(java.util.Calendar.MONTH, 1);
        }
        
        List<Transaction> txs = getByDateRange(startTime, endTime);
        for (Transaction tx : txs) {
            if ("DEBIT".equals(tx.getType())) {
                String monthStr = sdf.format(new java.util.Date(tx.getTimestamp()));
                if (totals.containsKey(monthStr)) {
                    totals.put(monthStr, totals.get(monthStr) + tx.getAmount());
                }
            }
        }
        return totals;
    }
}
