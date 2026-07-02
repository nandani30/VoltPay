package com.voltpay.backend.repository;

import com.voltpay.backend.model.ContactBackup;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface ContactsBackupRepository extends MongoRepository<ContactBackup, String> {
    Optional<ContactBackup> findByPhoneNumber(String phoneNumber);
}
