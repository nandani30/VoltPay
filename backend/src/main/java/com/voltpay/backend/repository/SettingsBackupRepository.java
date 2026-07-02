package com.voltpay.backend.repository;

import com.voltpay.backend.model.SettingsBackup;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface SettingsBackupRepository extends MongoRepository<SettingsBackup, String> {
    Optional<SettingsBackup> findByPhoneNumber(String phoneNumber);
}
