package com.voltpay.app.ui.contacts;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.voltpay.app.R;
import com.voltpay.app.data.db.ContactDao;
import com.voltpay.app.data.db.SyncItemDao;
import com.voltpay.app.data.model.Contact;
import com.voltpay.app.data.model.SyncItem;
import com.voltpay.app.sync.SyncWorker;

public class SaveContactPromptActivity extends AppCompatActivity {

    private String upiId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Transparent activity, no content view needed directly

        if (getIntent().hasExtra("EXTRA_UPI_ID")) {
            upiId = getIntent().getStringExtra("EXTRA_UPI_ID");
        }

        showBottomSheet();
    }

    private void showBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        
        // Inflate layout for bottom sheet
        View view = LayoutInflater.from(this).inflate(R.layout.activity_add_edit_contact, null);
        
        // Hide the top bar from the layout since it's a bottom sheet now
        view.findViewById(R.id.topBar).setVisibility(View.GONE);
        
        EditText etName = view.findViewById(R.id.etName);
        EditText etUpiId = view.findViewById(R.id.etUpiId);
        EditText etPhone = view.findViewById(R.id.etPhone);
        Button btnSave = view.findViewById(R.id.btnSave);
        
        // Pre-fill
        if (upiId.contains("@")) {
            etUpiId.setText(upiId);
        } else {
            // It might actually be the name if parsed from carrier message
            etName.setText(upiId);
        }
        
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String finalUpi = etUpiId.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            
            if (name.length() < 2) {
                Toast.makeText(this, "Name must be at least 2 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!finalUpi.matches("^[\\\\w.-]+@[\\\\w.-]+$")) {
                Toast.makeText(this, "Enter a valid UPI ID", Toast.LENGTH_SHORT).show();
                return;
            }
            
            ContactDao contactDao = new ContactDao(this);
            if (contactDao.getContactByUpiId(finalUpi) == null) {
                Contact newContact = new Contact(name, finalUpi, phone, System.currentTimeMillis());
                contactDao.insertContact(newContact);
                queueSync();
                Toast.makeText(this, "Contact saved", Toast.LENGTH_SHORT).show();
            }
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.setOnDismissListener(dialog -> finish());
        bottomSheetDialog.show();
    }

    private void queueSync() {
        SyncItemDao syncItemDao = new SyncItemDao(this);
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
                .setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueue(syncRequest);
    }
}
