package com.voltpay.app.ui.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.voltpay.app.R;
import com.voltpay.app.data.db.ContactDao;
import com.voltpay.app.data.db.DatabaseHelper;
import com.voltpay.app.data.db.SyncItemDao;
import com.voltpay.app.data.model.Contact;
import com.voltpay.app.data.model.SyncItem;
import com.voltpay.app.sync.SyncWorker;
import com.voltpay.app.ui.payment.SendMoneyActivity;

import java.util.List;

public class ContactsActivity extends AppCompatActivity {

    private ContactDao contactDao;
    private SyncItemDao syncItemDao;
    private ContactsAdapter adapter;
    private View emptyStateLayout;
    private RecyclerView rvContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        contactDao = new ContactDao(this);
        syncItemDao = new SyncItemDao(this);

        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        rvContacts = findViewById(R.id.rvContacts);
        EditText etSearch = findViewById(R.id.etSearch);
        ImageView btnBack = findViewById(R.id.btnBack);
        View fabAddContact = findViewById(R.id.fabAddContact);
        View btnAddContactEmpty = findViewById(R.id.btnAddContactEmpty);

        btnBack.setOnClickListener(v -> finish());
        
        View.OnClickListener addContactListener = v -> {
            Intent intent = new Intent(this, AddEditContactActivity.class);
            startActivity(intent);
        };
        
        if (fabAddContact != null) fabAddContact.setOnClickListener(addContactListener);
        if (btnAddContactEmpty != null) btnAddContactEmpty.setOnClickListener(addContactListener);

        setupRecyclerView();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchContacts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadContacts();
    }

    private void setupRecyclerView() {
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactsAdapter(new ContactsAdapter.OnContactClickListener() {
            @Override
            public void onContactClick(Contact contact) {
                Intent intent = new Intent(ContactsActivity.this, SendMoneyActivity.class);
                intent.putExtra("EXTRA_UPI_ID", contact.getUpiId());
                startActivity(intent);
            }

            @Override
            public void onContactLongClick(Contact contact, View view) {
                showContextMenu(contact, view);
            }
        });
        rvContacts.setAdapter(adapter);
    }

    private void loadContacts() {
        List<Contact> contacts = contactDao.getAllContacts();
        updateUI(contacts);
    }

    private void searchContacts(String query) {
        if (query.trim().isEmpty()) {
            loadContacts();
        } else {
            List<Contact> contacts = contactDao.searchByName(query.trim());
            updateUI(contacts);
        }
    }

    private void updateUI(List<Contact> contacts) {
        if (contacts.isEmpty()) {
            rvContacts.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            rvContacts.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            adapter.setContacts(contacts);
        }
    }

    private void showContextMenu(Contact contact, View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenu().add(0, 1, 0, "Edit");
        popupMenu.getMenu().add(0, 2, 1, "Delete");

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                Intent intent = new Intent(this, AddEditContactActivity.class);
                intent.putExtra("EXTRA_CONTACT_ID", contact.getId());
                startActivity(intent);
                return true;
            } else if (item.getItemId() == 2) {
                showDeleteConfirmation(contact);
                return true;
            }
            return false;
        });
        popupMenu.show();
    }

    private void showDeleteConfirmation(Contact contact) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Contact")
                .setMessage("Delete " + contact.getName() + "? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    contactDao.deleteContact(contact.getId());
                    queueSync();
                    loadContacts();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void queueSync() {
        long currentTime = System.currentTimeMillis();
        SyncItem existingItem = syncItemDao.getPendingItemByActionType("SYNC_CONTACTS");

        if (existingItem != null) {
            syncItemDao.updateSyncItemPayload(existingItem.getId(), "update_triggered", currentTime);
        } else {
            SyncItem newItem = new SyncItem();
            newItem.setActionType("SYNC_CONTACTS");
            newItem.setPayload("update_triggered");
            newItem.setCreatedAt(currentTime);
            newItem.setStatus("PENDING");
            syncItemDao.insertSyncItem(newItem);
        }

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        
        OneTimeWorkRequest syncRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(constraints)
                .build();
                
        WorkManager.getInstance(this).enqueue(syncRequest);
    }
}
