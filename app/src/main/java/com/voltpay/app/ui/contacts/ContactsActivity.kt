package com.voltpay.app.ui.contacts

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.voltpay.app.R
import com.voltpay.app.data.db.ContactDao
import com.voltpay.app.data.db.SyncItemDao
import com.voltpay.app.data.model.Contact
import com.voltpay.app.data.model.SyncItem
import com.voltpay.app.sync.SyncWorker
import com.voltpay.app.ui.payment.SendMoneyActivity

class ContactsActivity : AppCompatActivity() {

    private lateinit var contactDao: ContactDao
    private lateinit var syncItemDao: SyncItemDao
    private lateinit var adapter: ContactsAdapter
    private lateinit var emptyStateLayout: View
    private lateinit var rvContacts: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        contactDao = ContactDao(this)
        syncItemDao = SyncItemDao(this)

        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        rvContacts = findViewById(R.id.rvContacts)
        val etSearch = findViewById<EditText>(R.id.etSearch)
        
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        
        val addContactListener = View.OnClickListener {
            val intent = Intent(this, AddEditContactActivity::class.java)
            startActivity(intent)
        }
        
        findViewById<View>(R.id.fabAddContact)?.setOnClickListener(addContactListener)
        findViewById<View>(R.id.btnAddContactEmpty)?.setOnClickListener(addContactListener)

        setupRecyclerView()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                searchContacts(s.toString())
            }
            override fun afterTextChanged(s: Editable) {}
        })
    }

    override fun onResume() {
        super.onResume()
        loadContacts()
    }

    private fun setupRecyclerView() {
        rvContacts.layoutManager = LinearLayoutManager(this)
        adapter = ContactsAdapter(object : ContactsAdapter.OnContactClickListener {
            override fun onContactClick(contact: Contact) {
                val intent = Intent(this@ContactsActivity, SendMoneyActivity::class.java)
                intent.putExtra("EXTRA_UPI_ID", contact.upiId)
                startActivity(intent)
            }

            override fun onContactLongClick(contact: Contact, view: View) {
                showContextMenu(contact, view)
            }
        })
        rvContacts.adapter = adapter
    }

    private fun loadContacts() {
        val contacts = contactDao.getAllContacts()
        updateUI(contacts)
    }

    private fun searchContacts(query: String) {
        if (query.trim().isEmpty()) {
            loadContacts()
        } else {
            val contacts = contactDao.searchByName(query.trim())
            updateUI(contacts)
        }
    }

    private fun updateUI(contacts: List<Contact>) {
        if (contacts.isEmpty()) {
            rvContacts.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            rvContacts.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            adapter.setContacts(contacts)
        }
    }

    private fun showContextMenu(contact: Contact, view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menu.add(0, 1, 0, "Edit")
        popupMenu.menu.add(0, 2, 1, "Delete")

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    val intent = Intent(this, AddEditContactActivity::class.java)
                    intent.putExtra("EXTRA_CONTACT_ID", contact.id)
                    startActivity(intent)
                    true
                }
                2 -> {
                    showDeleteConfirmation(contact)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showDeleteConfirmation(contact: Contact) {
        AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Delete ${contact.name}? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                contactDao.deleteContact(contact.id)
                queueSync()
                loadContacts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun queueSync() {
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
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueue(syncRequest)
    }
}
