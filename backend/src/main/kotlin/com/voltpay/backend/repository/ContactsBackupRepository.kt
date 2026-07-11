package com.voltpay.backend.repository

import com.voltpay.backend.model.ContactBackup
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.Optional

interface ContactsBackupRepository : MongoRepository<ContactBackup, String> {
    fun findByPhoneNumber(phoneNumber: String): Optional<ContactBackup>
}
