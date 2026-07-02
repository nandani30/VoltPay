package com.voltpay.backend.service;

import com.voltpay.backend.model.ContactBackup;
import com.voltpay.backend.repository.ContactsBackupRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ContactsBackupService {

    private final ContactsBackupRepository repository;

    public ContactsBackupService(ContactsBackupRepository repository) {
        this.repository = repository;
    }

    public ContactBackup syncContacts(String phoneNumber, List<ContactBackup.ContactEntry> contacts) {
        Optional<ContactBackup> existingOpt = repository.findByPhoneNumber(phoneNumber);
        ContactBackup backup = existingOpt.orElse(new ContactBackup());
        
        backup.setPhoneNumber(phoneNumber);
        // Simple strategy: overwrite with client's list. 
        // In a real app we might do true merging, but client sync logic merges before sending.
        backup.setContacts(contacts);
        backup.setLastSyncedAt(Instant.now());
        
        return repository.save(backup);
    }

    public Optional<ContactBackup> getContacts(String phoneNumber) {
        return repository.findByPhoneNumber(phoneNumber);
    }
}
