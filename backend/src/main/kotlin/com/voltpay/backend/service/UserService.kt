package com.voltpay.backend.service

import com.voltpay.backend.model.User
import com.voltpay.backend.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.UUID

@Service
class UserService(private val userRepository: UserRepository) {

    fun login(phoneNumber: String, name: String?, upiId: String?): User {
        // Simplified dummy login for offline-first approach without Firebase.
        val optUser = userRepository.findByPhoneNumber(phoneNumber)
        val user = optUser.orElse(User())
        user.phoneNumber = phoneNumber
        
        name?.let { user.name = it }
        upiId?.let { user.upiId = it }
        
        val token = UUID.randomUUID().toString()
        user.authToken = token
        user.tokenExpiry = Instant.now().plus(30, ChronoUnit.DAYS)
        
        return userRepository.save(user)
    }

    fun refreshToken(phoneNumber: String, currentToken: String): String {
        val strippedToken = if (currentToken.startsWith("Bearer ")) currentToken.substring(7) else currentToken

        val optUser = userRepository.findByPhoneNumber(phoneNumber)
        if (optUser.isEmpty) throw IllegalArgumentException("User not found")

        val user = optUser.get()
        if (strippedToken != user.authToken) {
            throw IllegalArgumentException("Invalid token")
        }

        val newToken = UUID.randomUUID().toString()
        user.authToken = newToken
        user.tokenExpiry = Instant.now().plus(30, ChronoUnit.DAYS)
        userRepository.save(user)
        return newToken
    }

    fun getUserByPhone(phoneNumber: String): Optional<User> {
        return userRepository.findByPhoneNumber(phoneNumber)
    }

    fun validateToken(phoneNumber: String, token: String?): Boolean {
        if (token.isNullOrEmpty()) return false
        val strippedToken = if (token.startsWith("Bearer ")) token.substring(7) else token
        
        val userOpt = userRepository.findByPhoneNumber(phoneNumber)
        if (userOpt.isPresent) {
            val u = userOpt.get()
            if (strippedToken == u.authToken && u.tokenExpiry != null && Instant.now().isBefore(u.tokenExpiry)) {
                return true
            }
        }
        return false
    }
}
