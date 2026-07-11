package com.voltpay.app.ui.contacts

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.gson.Gson
import com.voltpay.app.R
import com.voltpay.app.data.db.ContactDao
import com.voltpay.app.data.db.SyncItemDao
import com.voltpay.app.data.model.Contact
import com.voltpay.app.data.model.SyncItem
import com.voltpay.app.sync.SyncWorker

class AddEditContactActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etUpiId: EditText
    private lateinit var etPhone: EditText
    private lateinit var tvUpiError: TextView
    private lateinit var tvTitle: TextView
    private lateinit var btnSave: Button
    
    private lateinit var contactDao: ContactDao
    private lateinit var syncItemDao: SyncItemDao
    
    private var contactId: Long = -1
    private var existingContact: Contact? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_contact)

        contactDao = ContactDao(this)
        syncItemDao = SyncItemDao(this)

        tvTitle = findViewById(R.id.tvTitle)
        etName = findViewById(R.id.etName)
        etUpiId = findViewById(R.id.etUpiId)
        etPhone = findViewById(R.id.etPhone)
        tvUpiError = findViewById(R.id.tvUpiError)
        btnSave = findViewById(R.id.btnSave)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        if (intent.hasExtra("EXTRA_CONTACT_ID")) {
            contactId = intent.getLongExtra("EXTRA_CONTACT_ID", -1)
            if (contactId != -1L) {
                existingContact = contactDao.getById(contactId)
                if (existingContact != null) {
                    tvTitle.text = "Edit Contact"
                    etName.setText(existingContact?.name)
                    etUpiId.setText(existingContact?.upiId)
                    etPhone.setText(existingContact?.phoneNumber)
                }
            }
        }

        etUpiId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                tvUpiError.visibility = View.GONE
            }
            override fun afterTextChanged(s: Editable) {}
        })

        btnSave.setOnClickListener { saveContact() }
    }

    private fun saveContact() {
        val name = etName.text.toString().trim()
        val upiId = etUpiId.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        if (name.length < 2) {
            Toast.makeText(this, "Name must be at least 2 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (!upiId.matches(Regex("^[\\w.-]+@[\\w.-]+$"))) {
            tvUpiError.visibility = View.VISIBLE
            tvUpiError.text = "Enter a valid UPI ID (e.g. name@okaxis)"
            return
        }

        val duplicateContact = contactDao.getContactByUpiId(upiId)
        if (duplicateContact != null) {
            if (existingContact == null || duplicateContact.id != existingContact?.id) {
                tvUpiError.visibility = View.VISIBLE
                tvUpiError.text = "This UPI ID is already saved as ${duplicateContact.name}"
                return
            }
        }

        if (existingContact != null) {
            existingContact?.name = name
            existingContact?.upiId = upiId
            existingContact?.phoneNumber = phone
            contactDao.updateContact(existingContact!!)
        } else {
            val newContact = Contact(name = name, upiId = upiId, phoneNumber = phone, createdAt = System.currentTimeMillis())
            contactDao.insertContact(newContact)
        }

        queueSync()
        
        Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun queueSync() {
        val currentTime = System.currentTimeMillis()
        val existingItem = syncItemDao.getPendingItemByActionType("SYNC_CONTACTS")
        
        val allContacts = contactDao.getAllContacts()
        val payloadJson = Gson().toJson(allContacts)

        if (existingItem != null) {
            syncItemDao.updateSyncItemPayload(existingItem.id, payloadJson, currentTime)
        } else {
            val newItem = SyncItem()
            newItem.actionType = "SYNC_CONTACTS"
            newItem.payload = payloadJson
            newItem.createdAt = currentTime
            newItem.status = "PENDING"
            syncItemDao.insertSyncItem(newItem)
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncRequest = OneTimeWorkRequest.Builder(SyncWorker::class.java)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueue(syncRequest)
    }
}
