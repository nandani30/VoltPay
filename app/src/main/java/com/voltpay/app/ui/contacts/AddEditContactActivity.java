package com.voltpay.app.ui.contacts;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.voltpay.app.R;
import com.voltpay.app.data.db.ContactDao;
import com.voltpay.app.data.db.SyncItemDao;
import com.voltpay.app.data.model.Contact;
import com.voltpay.app.data.model.SyncItem;
import com.voltpay.app.sync.SyncWorker;
import com.google.gson.Gson;

public class AddEditContactActivity extends AppCompatActivity {

    private EditText etName, etUpiId, etPhone;
    private TextView tvUpiError, tvTitle;
    private Button btnSave;
    
    private ContactDao contactDao;
    private SyncItemDao syncItemDao;
    
    private long contactId = -1;
    private Contact existingContact = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_contact);

        contactDao = new ContactDao(this);
        syncItemDao = new SyncItemDao(this);

        tvTitle = findViewById(R.id.tvTitle);
        etName = findViewById(R.id.etName);
        etUpiId = findViewById(R.id.etUpiId);
        etPhone = findViewById(R.id.etPhone);
        tvUpiError = findViewById(R.id.tvUpiError);
        btnSave = findViewById(R.id.btnSave);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        if (getIntent().hasExtra("EXTRA_CONTACT_ID")) {
            contactId = getIntent().getLongExtra("EXTRA_CONTACT_ID", -1);
            if (contactId != -1) {
                existingContact = contactDao.getById(contactId);
                if (existingContact != null) {
                    tvTitle.setText("Edit Contact");
                    etName.setText(existingContact.getName());
                    etUpiId.setText(existingContact.getUpiId());
                    etPhone.setText(existingContact.getPhoneNumber());
                }
            }
        }

        etUpiId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvUpiError.setVisibility(View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnSave.setOnClickListener(v -> saveContact());
    }

    private void saveContact() {
        String name = etName.getText().toString().trim();
        String upiId = etUpiId.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (name.length() < 2) {
            Toast.makeText(this, "Name must be at least 2 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!upiId.matches("^[\\\\w.-]+@[\\\\w.-]+$")) {
            tvUpiError.setVisibility(View.VISIBLE);
            tvUpiError.setText("Enter a valid UPI ID (e.g. name@okaxis)");
            return;
        }

        Contact duplicateContact = contactDao.getContactByUpiId(upiId);
        if (duplicateContact != null) {
            if (existingContact == null || duplicateContact.getId() != existingContact.getId()) {
                tvUpiError.setVisibility(View.VISIBLE);
                tvUpiError.setText("This UPI ID is already saved as " + duplicateContact.getName());
                return;
            }
        }

        if (existingContact != null) {
            existingContact.setName(name);
            existingContact.setUpiId(upiId);
            existingContact.setPhoneNumber(phone);
            contactDao.updateContact(existingContact);
        } else {
            Contact newContact = new Contact(name, upiId, phone, System.currentTimeMillis());
            contactDao.insertContact(newContact);
        }

        queueSync();
        
        Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void queueSync() {
        long currentTime = System.currentTimeMillis();
        SyncItem existingItem = syncItemDao.getPendingItemByActionType("SYNC_CONTACTS");
        
        java.util.List<Contact> allContacts = contactDao.getAllContacts();
        String payloadJson = new Gson().toJson(allContacts);

        if (existingItem != null) {
            syncItemDao.updateSyncItemPayload(existingItem.getId(), payloadJson, currentTime);
        } else {
            SyncItem newItem = new SyncItem();
            newItem.setActionType("SYNC_CONTACTS");
            newItem.setPayload(payloadJson);
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
