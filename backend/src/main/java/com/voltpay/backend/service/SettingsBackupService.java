package com.voltpay.backend.service;

import com.voltpay.backend.model.SettingsBackup;
import com.voltpay.backend.repository.SettingsBackupRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class SettingsBackupService {

    private final SettingsBackupRepository repository;

    public SettingsBackupService(SettingsBackupRepository repository) {
        this.repository = repository;
    }

    public SettingsBackup syncSettings(String phoneNumber, String displayName, String ownUpiId, Integer simSlotIndex) {
        Optional<SettingsBackup> existingOpt = repository.findByPhoneNumber(phoneNumber);
        SettingsBackup backup = existingOpt.orElse(new SettingsBackup());

        backup.setPhoneNumber(phoneNumber);
        backup.setDisplayName(displayName);
        backup.setOwnUpiId(ownUpiId);
        backup.setSimSlotIndex(simSlotIndex);
        backup.setUpdatedAt(Instant.now());

        return repository.save(backup);
    }

    public Optional<SettingsBackup> getSettings(String phoneNumber) {
        return repository.findByPhoneNumber(phoneNumber);
    }
}
