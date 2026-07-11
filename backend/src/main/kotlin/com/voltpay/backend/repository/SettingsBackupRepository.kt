package com.voltpay.backend.repository

import com.voltpay.backend.model.SettingsBackup
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.Optional

interface SettingsBackupRepository : MongoRepository<SettingsBackup, String> {
    fun findByPhoneNumber(phoneNumber: String): Optional<SettingsBackup>
}
