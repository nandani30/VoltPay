package com.voltpay.app.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import com.voltpay.app.data.model.SyncItem;

import java.util.ArrayList;
import java.util.List;

public class SyncItemDao {
    private final DatabaseHelper dbHelper;

    public SyncItemDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public long insertSyncItem(SyncItem item) {
        SQLiteDatabase db = dbHelper.getSecureWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SYNC_ACTION_TYPE, item.getActionType());
        values.put(DatabaseHelper.COL_SYNC_PAYLOAD, item.getPayload());
        values.put(DatabaseHelper.COL_SYNC_CREATED_AT, item.getCreatedAt());
        values.put(DatabaseHelper.COL_SYNC_RETRY_COUNT, item.getRetryCount());
        values.put(DatabaseHelper.COL_SYNC_LAST_ATTEMPTED, item.getLastAttempted());
        values.put(DatabaseHelper.COL_SYNC_STATUS, item.getStatus());
        
        long id = db.insert(DatabaseHelper.TABLE_SYNC_QUEUE, null, values);
        db.close();
        return id;
    }

    public List<SyncItem> getPendingItems() {
        List<SyncItem> items = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getSecureReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_SYNC_QUEUE, null, 
                DatabaseHelper.COL_SYNC_STATUS + " = ?", new String[]{"PENDING"}, 
                null, null, DatabaseHelper.COL_SYNC_CREATED_AT + " ASC");
        
        if (cursor.moveToFirst()) {
            do {
                SyncItem item = new SyncItem();
                item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_ID)));
                item.setActionType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_ACTION_TYPE)));
                item.setPayload(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_PAYLOAD)));
                item.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_CREATED_AT)));
                item.setRetryCount(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_RETRY_COUNT)));
                item.setLastAttempted(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_LAST_ATTEMPTED)));
                item.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_STATUS)));
                items.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return items;
    }

    public SyncItem getPendingItemByActionType(String actionType) {
        SQLiteDatabase db = dbHelper.getSecureReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_SYNC_QUEUE, null, 
                DatabaseHelper.COL_SYNC_STATUS + " = ? AND " + DatabaseHelper.COL_SYNC_ACTION_TYPE + " = ?", 
                new String[]{"PENDING", actionType}, 
                null, null, null);
        
        SyncItem item = null;
        if (cursor.moveToFirst()) {
            item = new SyncItem();
            item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_ID)));
            item.setActionType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_ACTION_TYPE)));
            item.setPayload(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_PAYLOAD)));
            item.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_CREATED_AT)));
            item.setRetryCount(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_RETRY_COUNT)));
            item.setLastAttempted(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_LAST_ATTEMPTED)));
            item.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_STATUS)));
        }
        cursor.close();
        db.close();
        return item;
    }

    public void updateSyncItemPayload(long id, String payload, long createdAt) {
        SQLiteDatabase db = dbHelper.getSecureWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SYNC_PAYLOAD, payload);
        values.put(DatabaseHelper.COL_SYNC_CREATED_AT, createdAt);
        
        db.update(DatabaseHelper.TABLE_SYNC_QUEUE, values, DatabaseHelper.COL_SYNC_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void updateSyncItemStatus(long id, String status, int retryCount, long lastAttempted) {
        SQLiteDatabase db = dbHelper.getSecureWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SYNC_STATUS, status);
        values.put(DatabaseHelper.COL_SYNC_RETRY_COUNT, retryCount);
        values.put(DatabaseHelper.COL_SYNC_LAST_ATTEMPTED, lastAttempted);
        
        db.update(DatabaseHelper.TABLE_SYNC_QUEUE, values, DatabaseHelper.COL_SYNC_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteSyncItem(long id) {
        SQLiteDatabase db = dbHelper.getSecureWritableDatabase();
        db.delete(DatabaseHelper.TABLE_SYNC_QUEUE, DatabaseHelper.COL_SYNC_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void markComplete(long id) {
        SQLiteDatabase db = dbHelper.getSecureWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SYNC_STATUS, "COMPLETE");
        db.update(DatabaseHelper.TABLE_SYNC_QUEUE, values, DatabaseHelper.COL_SYNC_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void markFailed(long id) {
        SQLiteDatabase db = dbHelper.getSecureWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SYNC_STATUS, "FAILED");
        db.update(DatabaseHelper.TABLE_SYNC_QUEUE, values, DatabaseHelper.COL_SYNC_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void incrementRetry(long id) {
        SQLiteDatabase db = dbHelper.getSecureWritableDatabase();
        // First get current retry count
        Cursor cursor = db.query(DatabaseHelper.TABLE_SYNC_QUEUE, new String[]{DatabaseHelper.COL_SYNC_RETRY_COUNT}, DatabaseHelper.COL_SYNC_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        int currentRetry = 0;
        if (cursor.moveToFirst()) {
            currentRetry = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_RETRY_COUNT));
        }
        cursor.close();
        
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_SYNC_RETRY_COUNT, currentRetry + 1);
        values.put(DatabaseHelper.COL_SYNC_LAST_ATTEMPTED, System.currentTimeMillis());
        db.update(DatabaseHelper.TABLE_SYNC_QUEUE, values, DatabaseHelper.COL_SYNC_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}
