package com.voltpay.backend.repository

import com.voltpay.backend.model.User
import org.springframework.data.mongodb.repository.MongoRepository
import java.util.Optional

interface UserRepository : MongoRepository<User, String> {
    fun findByPhoneNumber(phoneNumber: String): Optional<User>
    fun findByUpiId(upiId: String): Optional<User>
}
