package com.voltpay.app.data.db

import android.content.Context
import com.voltpay.app.utils.KeyStoreManager
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SQLiteOpenHelper

class DatabaseHelper private constructor(context: Context) : 
    SQLiteOpenHelper(context, DATABASE_NAME, KeyStoreManager.getDatabasePassphrase(context), null, DATABASE_VERSION, 0, null, null, false) {

    init {
        // Load the SQLCipher library
        System.loadLibrary("sqlcipher")
    }

    val secureWritableDatabase: SQLiteDatabase
        get() = writableDatabase as SQLiteDatabase

    val secureReadableDatabase: SQLiteDatabase
        get() = readableDatabase as SQLiteDatabase

    override fun onCreate(db: SQLiteDatabase) {
        // Create Contacts
        val createContacts = ("CREATE TABLE " + TABLE_CONTACTS + " (" +
                COL_CONTACT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CONTACT_NAME + " TEXT NOT NULL, " +
                COL_CONTACT_UPI_ID + " TEXT NOT NULL UNIQUE, " +
                COL_CONTACT_PHONE + " TEXT, " +
                COL_CONTACT_CREATED_AT + " INTEGER NOT NULL, " +
                COL_CONTACT_LAST_PAID_AT + " INTEGER" +
                ");")
        db.execSQL(createContacts)

        // Create Transactions
        val createTransactions = ("CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                COL_TX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TX_AMOUNT + " REAL NOT NULL, " +
                COL_TX_TYPE + " TEXT NOT NULL, " +
                COL_TX_UPI_REF + " TEXT, " +
                COL_TX_COUNTERPARTY + " TEXT, " +
                COL_TX_TIMESTAMP + " INTEGER NOT NULL, " +
                COL_TX_RAW_SMS + " TEXT, " +
                COL_TX_SOURCE + " TEXT DEFAULT 'SMS'" +
                ");")
        db.execSQL(createTransactions)

        // Create Sync Queue
        val createSyncQueue = ("CREATE TABLE " + TABLE_SYNC_QUEUE + " (" +
                COL_SYNC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SYNC_ACTION_TYPE + " TEXT NOT NULL, " +
                COL_SYNC_PAYLOAD + " TEXT NOT NULL, " +
                COL_SYNC_CREATED_AT + " INTEGER NOT NULL, " +
                COL_SYNC_RETRY_COUNT + " INTEGER DEFAULT 0, " +
                COL_SYNC_LAST_ATTEMPTED + " INTEGER, " +
                COL_SYNC_STATUS + " TEXT DEFAULT 'PENDING'" +
                ");")
        db.execSQL(createSyncQueue)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle specific migrations here instead of dropping tables
        // e.g., if (oldVersion < 2) { db.execSQL("ALTER TABLE ..."); }
    }

    companion object {
        private const val DATABASE_NAME = "voltpay_encrypted.db"
        private const val DATABASE_VERSION = 1

        // Contacts Table
        const val TABLE_CONTACTS = "contacts"
        const val COL_CONTACT_ID = "id"
        const val COL_CONTACT_NAME = "name"
        const val COL_CONTACT_UPI_ID = "upi_id"
        const val COL_CONTACT_PHONE = "phone_number"
        const val COL_CONTACT_CREATED_AT = "created_at"
        const val COL_CONTACT_LAST_PAID_AT = "last_paid_at"

        // Transactions Table
        const val TABLE_TRANSACTIONS = "transactions"
        const val COL_TX_ID = "id"
        const val COL_TX_AMOUNT = "amount"
        const val COL_TX_TYPE = "type"
        const val COL_TX_UPI_REF = "upi_ref"
        const val COL_TX_COUNTERPARTY = "counterparty"
        const val COL_TX_TIMESTAMP = "timestamp"
        const val COL_TX_RAW_SMS = "raw_sms"
        const val COL_TX_SOURCE = "source"

        // Sync Queue Table
        const val TABLE_SYNC_QUEUE = "sync_queue"
        const val COL_SYNC_ID = "id"
        const val COL_SYNC_ACTION_TYPE = "action_type"
        const val COL_SYNC_PAYLOAD = "payload"
        const val COL_SYNC_CREATED_AT = "created_at"
        const val COL_SYNC_RETRY_COUNT = "retry_count"
        const val COL_SYNC_LAST_ATTEMPTED = "last_attempted"
        const val COL_SYNC_STATUS = "status"

        @Volatile
        private var instance: DatabaseHelper? = null

        @Synchronized
        fun getInstance(context: Context): DatabaseHelper {
            if (instance == null) {
                instance = DatabaseHelper(context.applicationContext)
            }
            return instance!!
        }
    }
}
