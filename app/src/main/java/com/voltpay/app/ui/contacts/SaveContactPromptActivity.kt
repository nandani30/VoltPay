package com.voltpay.app.ui.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.voltpay.app.R
import com.voltpay.app.data.db.ContactDao
import com.voltpay.app.data.db.SyncItemDao
import com.voltpay.app.data.model.Contact
import com.voltpay.app.data.model.SyncItem
import com.voltpay.app.sync.SyncWorker

class SaveContactPromptActivity : AppCompatActivity() {

    private var upiId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent.hasExtra("EXTRA_UPI_ID")) {
            upiId = intent.getStringExtra("EXTRA_UPI_ID") ?: ""
        }

        showBottomSheet()
    }

    private fun showBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        
        val view = LayoutInflater.from(this).inflate(R.layout.activity_add_edit_contact, null)
        
        view.findViewById<View>(R.id.topBar).visibility = View.GONE
        
        val etName = view.findViewById<EditText>(R.id.etName)
        val etUpiId = view.findViewById<EditText>(R.id.etUpiId)
        val etPhone = view.findViewById<EditText>(R.id.etPhone)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        
        if (upiId.contains("@")) {
            etUpiId.setText(upiId)
        } else {
            etName.setText(upiId)
        }
        
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val finalUpi = etUpiId.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            
            if (name.length < 2) {
                Toast.makeText(this, "Name must be at least 2 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!finalUpi.matches(Regex("^[\\w.-]+@[\\w.-]+$"))) {
                Toast.makeText(this, "Enter a valid UPI ID", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val contactDao = ContactDao(this)
            if (contactDao.getContactByUpiId(finalUpi) == null) {
                val newContact = Contact(name = name, upiId = finalUpi, phoneNumber = phone, createdAt = System.currentTimeMillis())
                contactDao.insertContact(newContact)
                queueSync()
                Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show()
            }
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.setOnDismissListener { finish() }
        bottomSheetDialog.show()
    }

    private fun queueSync() {
        val syncItemDao = SyncItemDao(this)
        val currentTime = System.currentTimeMillis()
        val existingItem = syncItemDao.getPendingItemByActionType("SYNC_CONTACTS")

        if (existingItem != null) {
            syncItemDao.updateSyncItemPayload(existingItem.id, "update_triggered", currentTime)
        } else {
            val newItem = SyncItem()
            newItem.actionType = "SYNC_CONTACTS"
            newItem.payload = "update_triggered"
            newItem.createdAt = currentTime
            newItem.status = "PENDING"
            syncItemDao.insertSyncItem(newItem)
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val syncRequest = OneTimeWorkRequest.Builder(SyncWorker::class.java)
            .setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueue(syncRequest)
    }
}
