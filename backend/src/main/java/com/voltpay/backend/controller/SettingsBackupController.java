package com.voltpay.backend.controller;

import com.voltpay.backend.model.SettingsBackup;
import com.voltpay.backend.service.SettingsBackupService;
import com.voltpay.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingsBackupController {

    private final SettingsBackupService settingsService;
    private final UserService userService;

    public SettingsBackupController(SettingsBackupService settingsService, UserService userService) {
        this.settingsService = settingsService;
        this.userService = userService;
    }

    @PutMapping("/sync")
    public ResponseEntity<?> syncSettings(@RequestHeader("Authorization") String token,
                                          @RequestHeader("X-Phone-Number") String phoneNumber,
                                          @RequestBody SyncSettingsDto dto) {
        if (!userService.validateToken(phoneNumber, token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        SettingsBackup backup = settingsService.syncSettings(
                phoneNumber, dto.getDisplayName(), dto.getOwnUpiId(), dto.getSimSlotIndex());
        return ResponseEntity.ok(Map.of("success", true, "updatedAt", backup.getUpdatedAt()));
    }

    @GetMapping("/restore/{phoneNumber}")
    public ResponseEntity<?> restoreSettings(@RequestHeader("Authorization") String token,
                                             @PathVariable String phoneNumber) {
        if (!userService.validateToken(phoneNumber, token)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        return settingsService.getSettings(phoneNumber)
                .map(backup -> ResponseEntity.ok(Map.of(
                        "success", true,
                        "displayName", backup.getDisplayName() != null ? backup.getDisplayName() : "",
                        "ownUpiId", backup.getOwnUpiId() != null ? backup.getOwnUpiId() : "",
                        "simSlotIndex", backup.getSimSlotIndex() != null ? backup.getSimSlotIndex() : 0,
                        "updatedAt", backup.getUpdatedAt()
                )))
                .orElse(ResponseEntity.ok(Map.of(
                        "success", true,
                        "displayName", "",
                        "ownUpiId", "",
                        "simSlotIndex", 0,
                        "updatedAt", java.time.Instant.EPOCH
                )));
    }

    static class SyncSettingsDto {
        private String displayName;
        private String ownUpiId;
        private Integer simSlotIndex;
        
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        
        public String getOwnUpiId() { return ownUpiId; }
        public void setOwnUpiId(String ownUpiId) { this.ownUpiId = ownUpiId; }
        
        public Integer getSimSlotIndex() { return simSlotIndex; }
        public void setSimSlotIndex(Integer simSlotIndex) { this.simSlotIndex = simSlotIndex; }
    }
}
