package com.voltpay.backend.controller

import com.voltpay.backend.model.ContactBackup
import com.voltpay.backend.service.ContactsBackupService
import com.voltpay.backend.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@RequestMapping("/api/contacts")
class ContactsBackupController(
    private val contactsService: ContactsBackupService,
    private val userService: UserService
) {

    @PutMapping("/sync")
    fun syncContacts(
        @RequestHeader("Authorization") token: String,
        @RequestHeader("X-Phone-Number") phoneNumber: String,
        @RequestBody dto: SyncContactsDto
    ): ResponseEntity<*> {
        if (!userService.validateToken(phoneNumber, token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        }
        val backup = contactsService.syncContacts(phoneNumber, dto.contacts ?: emptyList())
        return ResponseEntity.ok(mapOf("success" to true, "lastSyncedAt" to backup.lastSyncedAt))
    }

    @GetMapping("/restore/{phoneNumber}")
    fun restoreContacts(
        @RequestHeader("Authorization") token: String,
        @PathVariable phoneNumber: String
    ): ResponseEntity<*> {
        if (!userService.validateToken(phoneNumber, token)) {
            return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized"))
        }
        val backup = contactsService.getBackup(phoneNumber)
        return if (backup != null) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "contacts" to backup.contacts,
                "lastSyncedAt" to backup.lastSyncedAt
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "contacts" to emptyList<ContactBackup.ContactEntry>(),
                "lastSyncedAt" to Instant.EPOCH
            ))
        }
    }

    data class SyncContactsDto(
        var contacts: List<ContactBackup.ContactEntry>? = null
    )
}
