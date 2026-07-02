package com.voltpay.backend.controller;

import com.voltpay.backend.model.ContactBackup;
import com.voltpay.backend.service.ContactsBackupService;
import com.voltpay.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contacts")
public class ContactsBackupController {

    private final ContactsBackupService contactsService;
    private final UserService userService;

    public ContactsBackupController(ContactsBackupService contactsService, UserService userService) {
        this.contactsService = contactsService;
        this.userService = userService;
    }

    @PutMapping("/sync")
    public ResponseEntity<?> syncContacts(@RequestHeader("Authorization") String token,
                                          @RequestHeader("X-Phone-Number") String phoneNumber,
                                          @RequestBody SyncContactsDto dto) {
        if (!userService.validateToken(phoneNumber, token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        ContactBackup backup = contactsService.syncContacts(phoneNumber, dto.getContacts());
        return ResponseEntity.ok(Map.of("success", true, "syncedAt", backup.getLastSyncedAt()));
    }

    @GetMapping("/restore/{phoneNumber}")
    public ResponseEntity<?> restoreContacts(@RequestHeader("Authorization") String token,
                                             @PathVariable String phoneNumber) {
        if (!userService.validateToken(phoneNumber, token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        return contactsService.getContacts(phoneNumber)
                .map(backup -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "contacts", backup.getContacts(),
                        "lastSyncedAt", backup.getLastSyncedAt()
                )))
                .orElse(ResponseEntity.ok(Map.of(
                        "success", true,
                        "contacts", List.of(),
                        "lastSyncedAt", Instant.EPOCH
                )));
    }

    static class SyncContactsDto {
        private List<ContactBackup.ContactEntry> contacts;
        
        public List<ContactBackup.ContactEntry> getContacts() { return contacts; }
        public void setContacts(List<ContactBackup.ContactEntry> contacts) { this.contacts = contacts; }
    }
}
