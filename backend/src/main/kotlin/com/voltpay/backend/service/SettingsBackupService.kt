package com.voltpay.backend.service

import com.voltpay.backend.model.SettingsBackup
import com.voltpay.backend.repository.SettingsBackupRepository
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SettingsBackupService(private val repository: SettingsBackupRepository) {

    fun syncSettings(phoneNumber: String, displayName: String?, ownUpiId: String?, simSlotIndex: Int?): SettingsBackup {
        val backup = repository.findByPhoneNumber(phoneNumber).orElse(SettingsBackup())
        backup.phoneNumber = phoneNumber
        backup.displayName = displayName
        backup.ownUpiId = ownUpiId
        backup.simSlotIndex = simSlotIndex
        backup.updatedAt = Instant.now()
        return repository.save(backup)
    }

    fun getSettings(phoneNumber: String): SettingsBackup? {
        return repository.findByPhoneNumber(phoneNumber).orElse(null)
    }
}
