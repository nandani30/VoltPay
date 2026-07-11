package com.voltpay.app.data.db

import android.content.ContentValues
import android.content.Context
import com.voltpay.app.data.model.SyncItem
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.util.ArrayList

class SyncItemDao(context: Context) {
    private val dbHelper: DatabaseHelper = DatabaseHelper.getInstance(context)

    fun insertSyncItem(item: SyncItem): Long {
        val db = dbHelper.secureWritableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_SYNC_ACTION_TYPE, item.actionType)
            put(DatabaseHelper.COL_SYNC_PAYLOAD, item.payload)
            put(DatabaseHelper.COL_SYNC_CREATED_AT, item.createdAt)
            put(DatabaseHelper.COL_SYNC_RETRY_COUNT, item.retryCount)
            put(DatabaseHelper.COL_SYNC_LAST_ATTEMPTED, item.lastAttempted)
            put(DatabaseHelper.COL_SYNC_STATUS, item.status)
        }
        val id = db.insert(DatabaseHelper.TABLE_SYNC_QUEUE, null, values)
        db.close()
        return id
    }

    val pendingItems: List<SyncItem>
        get() {
            val items = ArrayList<SyncItem>()
            val db = dbHelper.secureReadableDatabase
            val cursor = db.query(DatabaseHelper.TABLE_SYNC_QUEUE, null, 
                    DatabaseHelper.COL_SYNC_STATUS + " = ?", arrayOf("PENDING"), 
                    null, null, DatabaseHelper.COL_SYNC_CREATED_AT + " ASC")
            
            if (cursor.moveToFirst()) {
                do {
                    val item = SyncItem()
                    item.id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_ID))
                    item.actionType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_ACTION_TYPE))
                    item.payload = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_PAYLOAD))
                    item.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_CREATED_AT))
                    item.retryCount = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_RETRY_COUNT))
                    item.lastAttempted = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_LAST_ATTEMPTED))
                    item.status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_STATUS))
                    items.add(item)
                } while (cursor.moveToNext())
            }
            cursor.close()
            db.close()
            return items
        }

    fun getPendingItemByActionType(actionType: String): SyncItem? {
        val db = dbHelper.secureReadableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_SYNC_QUEUE, null, 
                DatabaseHelper.COL_SYNC_STATUS + " = ? AND " + DatabaseHelper.COL_SYNC_ACTION_TYPE + " = ?", 
                arrayOf("PENDING", actionType), 
                null, null, null)
        
        var item: SyncItem? = null
        if (cursor.moveToFirst()) {
            item = SyncItem()
            item.id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_ID))
            item.actionType = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_ACTION_TYPE))
            item.payload = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_PAYLOAD))
            item.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_CREATED_AT))
            item.retryCount = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_RETRY_COUNT))
            item.lastAttempted = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_LAST_ATTEMPTED))
            item.status = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_STATUS))
        }
        cursor.close()
        db.close()
        return item
    }

    fun updateSyncItemPayload(id: Long, payload: String, createdAt: Long) {
        val db = dbHelper.secureWritableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_SYNC_PAYLOAD, payload)
            put(DatabaseHelper.COL_SYNC_CREATED_AT, createdAt)
        }
        db.update(DatabaseHelper.TABLE_SYNC_QUEUE, values, DatabaseHelper.COL_SYNC_ID + " = ?", arrayOf(id.toString()))
        db.close()
    }

    fun updateSyncItemStatus(id: Long, status: String, retryCount: Int, lastAttempted: Long) {
        val db = dbHelper.secureWritableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_SYNC_STATUS, status)
            put(DatabaseHelper.COL_SYNC_RETRY_COUNT, retryCount)
            put(DatabaseHelper.COL_SYNC_LAST_ATTEMPTED, lastAttempted)
        }
        db.update(DatabaseHelper.TABLE_SYNC_QUEUE, values, DatabaseHelper.COL_SYNC_ID + " = ?", arrayOf(id.toString()))
        db.close()
    }

    fun deleteSyncItem(id: Long) {
        val db = dbHelper.secureWritableDatabase
        db.delete(DatabaseHelper.TABLE_SYNC_QUEUE, DatabaseHelper.COL_SYNC_ID + " = ?", arrayOf(id.toString()))
        db.close()
    }

    fun markComplete(id: Long) {
        val db = dbHelper.secureWritableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_SYNC_STATUS, "COMPLETE")
        }
        db.update(DatabaseHelper.TABLE_SYNC_QUEUE, values, DatabaseHelper.COL_SYNC_ID + " = ?", arrayOf(id.toString()))
        db.close()
    }

    fun markFailed(id: Long) {
        val db = dbHelper.secureWritableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_SYNC_STATUS, "FAILED")
        }
        db.update(DatabaseHelper.TABLE_SYNC_QUEUE, values, DatabaseHelper.COL_SYNC_ID + " = ?", arrayOf(id.toString()))
        db.close()
    }

    fun incrementRetry(id: Long) {
        val db = dbHelper.secureWritableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_SYNC_QUEUE, arrayOf(DatabaseHelper.COL_SYNC_RETRY_COUNT), DatabaseHelper.COL_SYNC_ID + " = ?", arrayOf(id.toString()), null, null, null)
        var currentRetry = 0
        if (cursor.moveToFirst()) {
            currentRetry = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SYNC_RETRY_COUNT))
        }
        cursor.close()
        
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_SYNC_RETRY_COUNT, currentRetry + 1)
            put(DatabaseHelper.COL_SYNC_LAST_ATTEMPTED, System.currentTimeMillis())
        }
        db.update(DatabaseHelper.TABLE_SYNC_QUEUE, values, DatabaseHelper.COL_SYNC_ID + " = ?", arrayOf(id.toString()))
        db.close()
    }
}
