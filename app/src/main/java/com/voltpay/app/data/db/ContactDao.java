package com.voltpay.app.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import com.voltpay.app.data.model.Contact;

import java.util.ArrayList;
import java.util.List;

public class ContactDao {
    private final DatabaseHelper dbHelper;

    public ContactDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    public long insertContact(Contact contact) {
        SQLiteDatabase db = dbHelper.getSecureWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_CONTACT_NAME, contact.getName());
        values.put(DatabaseHelper.COL_CONTACT_UPI_ID, contact.getUpiId());
        values.put(DatabaseHelper.COL_CONTACT_PHONE, contact.getPhoneNumber());
        values.put(DatabaseHelper.COL_CONTACT_CREATED_AT, contact.getCreatedAt());
        values.put(DatabaseHelper.COL_CONTACT_LAST_PAID_AT, contact.getLastPaidAt());
        
        long id = db.insertWithOnConflict(DatabaseHelper.TABLE_CONTACTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
        return id;
    }

    public List<Contact> getAllContacts() {
        List<Contact> contacts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getSecureReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CONTACTS, null, null, null, null, null, DatabaseHelper.COL_CONTACT_NAME + " ASC");
        
        if (cursor.moveToFirst()) {
            do {
                Contact contact = new Contact();
                contact.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_ID)));
                contact.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_NAME)));
                contact.setUpiId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_UPI_ID)));
                contact.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_PHONE)));
                contact.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_CREATED_AT)));
                contact.setLastPaidAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_LAST_PAID_AT)));
                contacts.add(contact);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return contacts;
    }

    public int updateContact(Contact contact) {
        SQLiteDatabase db = dbHelper.getSecureWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_CONTACT_NAME, contact.getName());
        values.put(DatabaseHelper.COL_CONTACT_UPI_ID, contact.getUpiId());
        values.put(DatabaseHelper.COL_CONTACT_PHONE, contact.getPhoneNumber());
        values.put(DatabaseHelper.COL_CONTACT_LAST_PAID_AT, contact.getLastPaidAt());
        
        int rows = db.update(DatabaseHelper.TABLE_CONTACTS, values, DatabaseHelper.COL_CONTACT_ID + " = ?", new String[]{String.valueOf(contact.getId())});
        db.close();
        return rows;
    }

    public int deleteContact(long id) {
        SQLiteDatabase db = dbHelper.getSecureWritableDatabase();
        int rows = db.delete(DatabaseHelper.TABLE_CONTACTS, DatabaseHelper.COL_CONTACT_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    public Contact getById(long id) {
        SQLiteDatabase db = dbHelper.getSecureReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CONTACTS, null, DatabaseHelper.COL_CONTACT_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        Contact contact = null;
        if (cursor.moveToFirst()) {
            contact = new Contact();
            contact.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_ID)));
            contact.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_NAME)));
            contact.setUpiId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_UPI_ID)));
            contact.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_PHONE)));
            contact.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_CREATED_AT)));
            contact.setLastPaidAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_LAST_PAID_AT)));
        }
        cursor.close();
        db.close();
        return contact;
    }

    public Contact getContactByUpiId(String upiId) {
        SQLiteDatabase db = dbHelper.getSecureReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CONTACTS, null, DatabaseHelper.COL_CONTACT_UPI_ID + " = ?", new String[]{upiId}, null, null, null);
        Contact contact = null;
        if (cursor.moveToFirst()) {
            contact = new Contact();
            contact.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_ID)));
            contact.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_NAME)));
            contact.setUpiId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_UPI_ID)));
            contact.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_PHONE)));
            contact.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_CREATED_AT)));
            contact.setLastPaidAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_LAST_PAID_AT)));
        }
        cursor.close();
        db.close();
        return contact;
    }

    public void updateLastPaidAt(String upiId, long timestamp) {
        SQLiteDatabase db = dbHelper.getSecureWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COL_CONTACT_LAST_PAID_AT, timestamp);
        db.update(DatabaseHelper.TABLE_CONTACTS, values, DatabaseHelper.COL_CONTACT_UPI_ID + " = ?", new String[]{upiId});
        db.close();
    }

    public List<Contact> searchByName(String query) {
        List<Contact> contacts = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getSecureReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_CONTACTS, null, DatabaseHelper.COL_CONTACT_NAME + " LIKE ?", new String[]{"%" + query + "%"}, null, null, DatabaseHelper.COL_CONTACT_NAME + " ASC");
        if (cursor.moveToFirst()) {
            do {
                Contact contact = new Contact();
                contact.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_ID)));
                contact.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_NAME)));
                contact.setUpiId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_UPI_ID)));
                contact.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_PHONE)));
                contact.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_CREATED_AT)));
                contact.setLastPaidAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_LAST_PAID_AT)));
                contacts.add(contact);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return contacts;
    }
}
