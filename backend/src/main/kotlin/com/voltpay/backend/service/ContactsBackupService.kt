package com.voltpay.backend.service

import com.voltpay.backend.model.ContactBackup
import com.voltpay.backend.repository.ContactsBackupRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ContactsBackupService(private val repository: ContactsBackupRepository) {

    fun syncContacts(phoneNumber: String, contacts: List<ContactBackup.ContactEntry>): ContactBackup {
        val backup = repository.findByPhoneNumber(phoneNumber).orElse(ContactBackup())
        backup.phoneNumber = phoneNumber
        backup.contacts = contacts
        backup.lastSyncedAt = Instant.now()
        return repository.save(backup)
    }

    fun getBackup(phoneNumber: String): ContactBackup? {
        return repository.findByPhoneNumber(phoneNumber).orElse(null)
    }
}
