package com.voltpay.app.data.db;

import android.content.Context;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;
import com.voltpay.app.utils.KeyStoreManager;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "voltpay_encrypted.db";
    private static final int DATABASE_VERSION = 1;

    // Contacts Table
    public static final String TABLE_CONTACTS = "contacts";
    public static final String COL_CONTACT_ID = "id";
    public static final String COL_CONTACT_NAME = "name";
    public static final String COL_CONTACT_UPI_ID = "upi_id";
    public static final String COL_CONTACT_PHONE = "phone_number";
    public static final String COL_CONTACT_CREATED_AT = "created_at";
    public static final String COL_CONTACT_LAST_PAID_AT = "last_paid_at";

    // Transactions Table
    public static final String TABLE_TRANSACTIONS = "transactions";
    public static final String COL_TX_ID = "id";
    public static final String COL_TX_AMOUNT = "amount";
    public static final String COL_TX_TYPE = "type";
    public static final String COL_TX_UPI_REF = "upi_ref";
    public static final String COL_TX_COUNTERPARTY = "counterparty";
    public static final String COL_TX_TIMESTAMP = "timestamp";
    public static final String COL_TX_RAW_SMS = "raw_sms";
    public static final String COL_TX_SOURCE = "source";

    // Sync Queue Table
    public static final String TABLE_SYNC_QUEUE = "sync_queue";
    public static final String COL_SYNC_ID = "id";
    public static final String COL_SYNC_ACTION_TYPE = "action_type";
    public static final String COL_SYNC_PAYLOAD = "payload";
    public static final String COL_SYNC_CREATED_AT = "created_at";
    public static final String COL_SYNC_RETRY_COUNT = "retry_count";
    public static final String COL_SYNC_LAST_ATTEMPTED = "last_attempted";
    public static final String COL_SYNC_STATUS = "status";

    private static DatabaseHelper instance;
    private final Context context;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    static {
        System.loadLibrary("sqlcipher");
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, KeyStoreManager.getDatabasePassphrase(context), null, DATABASE_VERSION, 0, null, null, false);
        this.context = context;
    }

    public SQLiteDatabase getSecureWritableDatabase() {
        return (SQLiteDatabase) getWritableDatabase();
    }

    public SQLiteDatabase getSecureReadableDatabase() {
        return (SQLiteDatabase) getReadableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Contacts
        String createContacts = "CREATE TABLE " + TABLE_CONTACTS + " (" +
                COL_CONTACT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_CONTACT_NAME + " TEXT NOT NULL, " +
                COL_CONTACT_UPI_ID + " TEXT NOT NULL UNIQUE, " +
                COL_CONTACT_PHONE + " TEXT, " +
                COL_CONTACT_CREATED_AT + " INTEGER NOT NULL, " +
                COL_CONTACT_LAST_PAID_AT + " INTEGER" +
                ");";
        db.execSQL(createContacts);

        // Create Transactions
        String createTransactions = "CREATE TABLE " + TABLE_TRANSACTIONS + " (" +
                COL_TX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TX_AMOUNT + " REAL NOT NULL, " +
                COL_TX_TYPE + " TEXT NOT NULL, " +
                COL_TX_UPI_REF + " TEXT, " +
                COL_TX_COUNTERPARTY + " TEXT, " +
                COL_TX_TIMESTAMP + " INTEGER NOT NULL, " +
                COL_TX_RAW_SMS + " TEXT, " +
                COL_TX_SOURCE + " TEXT DEFAULT 'SMS'" +
                ");";
        db.execSQL(createTransactions);

        // Create Sync Queue
        String createSyncQueue = "CREATE TABLE " + TABLE_SYNC_QUEUE + " (" +
                COL_SYNC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SYNC_ACTION_TYPE + " TEXT NOT NULL, " +
                COL_SYNC_PAYLOAD + " TEXT NOT NULL, " +
                COL_SYNC_CREATED_AT + " INTEGER NOT NULL, " +
                COL_SYNC_RETRY_COUNT + " INTEGER DEFAULT 0, " +
                COL_SYNC_LAST_ATTEMPTED + " INTEGER, " +
                COL_SYNC_STATUS + " TEXT DEFAULT 'PENDING'" +
                ");";
        db.execSQL(createSyncQueue);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle specific migrations here instead of dropping tables
        // e.g., if (oldVersion < 2) { db.execSQL("ALTER TABLE ..."); }
    }
}
