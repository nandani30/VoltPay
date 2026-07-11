package com.voltpay.backend.controller

import com.voltpay.backend.service.SettingsBackupService
import com.voltpay.backend.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/settings")
class SettingsBackupController(
    private val settingsService: SettingsBackupService,
    private val userService: UserService
) {

    @PutMapping("/sync")
    fun syncSettings(
        @RequestHeader("Authorization") token: String,
        @RequestHeader("X-Phone-Number") phoneNumber: String,
        @RequestBody dto: SyncSettingsDto
    ): ResponseEntity<*> {
        if (!userService.validateToken(phoneNumber, token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        }
        val backup = settingsService.syncSettings(
            phoneNumber, dto.displayName, dto.ownUpiId, dto.simSlotIndex
        )
        return ResponseEntity.ok(mapOf("success" to true, "updatedAt" to backup.updatedAt))
    }

    @GetMapping("/restore/{phoneNumber}")
    fun restoreSettings(
        @RequestHeader("Authorization") token: String,
        @PathVariable phoneNumber: String
    ): ResponseEntity<*> {
        if (!userService.validateToken(phoneNumber, token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        }
        val backup = settingsService.getSettings(phoneNumber)
        return if (backup != null) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "displayName" to (backup.displayName ?: ""),
                "ownUpiId" to (backup.ownUpiId ?: ""),
                "simSlotIndex" to (backup.simSlotIndex ?: 0),
                "updatedAt" to backup.updatedAt
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "displayName" to "",
                "ownUpiId" to "",
                "simSlotIndex" to 0,
                "updatedAt" to Instant.EPOCH
            ))
        }
    }

    data class SyncSettingsDto(
        var displayName: String? = null,
        var ownUpiId: String? = null,
        var simSlotIndex: Int? = null
    )
}
