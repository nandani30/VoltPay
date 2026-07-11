package com.voltpay.app.data.db

import android.content.ContentValues
import android.content.Context
import com.voltpay.app.data.model.Contact
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.util.ArrayList

class ContactDao(context: Context) {
    private val dbHelper: DatabaseHelper = DatabaseHelper.getInstance(context)

    fun insertContact(contact: Contact): Long {
        val db = dbHelper.secureWritableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_CONTACT_NAME, contact.name)
            put(DatabaseHelper.COL_CONTACT_UPI_ID, contact.upiId)
            put(DatabaseHelper.COL_CONTACT_PHONE, contact.phoneNumber)
            put(DatabaseHelper.COL_CONTACT_CREATED_AT, contact.createdAt)
            put(DatabaseHelper.COL_CONTACT_LAST_PAID_AT, contact.lastPaidAt)
        }
        val id = db.insertWithOnConflict(DatabaseHelper.TABLE_CONTACTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
        return id
    }

    fun getAllContacts(): List<Contact> {
        val contacts = ArrayList<Contact>()
        val db = dbHelper.secureReadableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_CONTACTS, null, null, null, null, null, DatabaseHelper.COL_CONTACT_NAME + " ASC")
        
        if (cursor.moveToFirst()) {
            do {
                val contact = Contact()
                contact.id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_ID))
                contact.name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_NAME))
                contact.upiId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_UPI_ID))
                contact.phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_PHONE))
                contact.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_CREATED_AT))
                contact.lastPaidAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_LAST_PAID_AT))
                contacts.add(contact)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return contacts
    }

    fun updateContact(contact: Contact): Int {
        val db = dbHelper.secureWritableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_CONTACT_NAME, contact.name)
            put(DatabaseHelper.COL_CONTACT_UPI_ID, contact.upiId)
            put(DatabaseHelper.COL_CONTACT_PHONE, contact.phoneNumber)
            put(DatabaseHelper.COL_CONTACT_LAST_PAID_AT, contact.lastPaidAt)
        }
        val rows = db.update(DatabaseHelper.TABLE_CONTACTS, values, DatabaseHelper.COL_CONTACT_ID + " = ?", arrayOf(contact.id.toString()))
        db.close()
        return rows
    }

    fun deleteContact(id: Long): Int {
        val db = dbHelper.secureWritableDatabase
        val rows = db.delete(DatabaseHelper.TABLE_CONTACTS, DatabaseHelper.COL_CONTACT_ID + " = ?", arrayOf(id.toString()))
        db.close()
        return rows
    }

    fun getById(id: Long): Contact? {
        val db = dbHelper.secureReadableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_CONTACTS, null, DatabaseHelper.COL_CONTACT_ID + " = ?", arrayOf(id.toString()), null, null, null)
        var contact: Contact? = null
        if (cursor.moveToFirst()) {
            contact = Contact()
            contact.id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_ID))
            contact.name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_NAME))
            contact.upiId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_UPI_ID))
            contact.phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_PHONE))
            contact.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_CREATED_AT))
            contact.lastPaidAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_LAST_PAID_AT))
        }
        cursor.close()
        db.close()
        return contact
    }

    fun getContactByUpiId(upiId: String): Contact? {
        val db = dbHelper.secureReadableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_CONTACTS, null, DatabaseHelper.COL_CONTACT_UPI_ID + " = ?", arrayOf(upiId), null, null, null)
        var contact: Contact? = null
        if (cursor.moveToFirst()) {
            contact = Contact()
            contact.id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_ID))
            contact.name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_NAME))
            contact.upiId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_UPI_ID))
            contact.phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_PHONE))
            contact.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_CREATED_AT))
            contact.lastPaidAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_LAST_PAID_AT))
        }
        cursor.close()
        db.close()
        return contact
    }

    fun updateLastPaidAt(upiId: String, timestamp: Long) {
        val db = dbHelper.secureWritableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COL_CONTACT_LAST_PAID_AT, timestamp)
        }
        db.update(DatabaseHelper.TABLE_CONTACTS, values, DatabaseHelper.COL_CONTACT_UPI_ID + " = ?", arrayOf(upiId))
        db.close()
    }

    fun searchByName(query: String): List<Contact> {
        val contacts = ArrayList<Contact>()
        val db = dbHelper.secureReadableDatabase
        val cursor = db.query(DatabaseHelper.TABLE_CONTACTS, null, DatabaseHelper.COL_CONTACT_NAME + " LIKE ?", arrayOf("%$query%"), null, null, DatabaseHelper.COL_CONTACT_NAME + " ASC")
        if (cursor.moveToFirst()) {
            do {
                val contact = Contact()
                contact.id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_ID))
                contact.name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_NAME))
                contact.upiId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_UPI_ID))
                contact.phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_PHONE))
                contact.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_CREATED_AT))
                contact.lastPaidAt = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CONTACT_LAST_PAID_AT))
                contacts.add(contact)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return contacts
    }
}
