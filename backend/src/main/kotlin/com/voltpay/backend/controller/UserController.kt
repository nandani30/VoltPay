package com.voltpay.backend.controller

import com.voltpay.backend.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(private val userService: UserService) {

    @PostMapping("/login")
    fun login(@RequestBody dto: LoginDto): ResponseEntity<*> {
        try {
            val user = userService.login(dto.phoneNumber ?: "", dto.name, dto.upiId)
            return ResponseEntity.ok(mapOf(
                "success" to true,
                "userId" to user.id,
                "token" to user.authToken
            ))
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(mapOf("success" to false, "error" to e.message))
        }
    }

    @PostMapping("/refresh-token")
    fun refreshToken(
        @RequestHeader("Authorization") token: String,
        @RequestBody dto: PhoneDto
    ): ResponseEntity<*> {
        return try {
            val newToken = userService.refreshToken(dto.phoneNumber ?: "", token)
            ResponseEntity.ok(mapOf("token" to newToken))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(401).body(mapOf("error" to e.message))
        }
    }

    data class PhoneDto(
        var phoneNumber: String? = null
    )

    data class LoginDto(
        var phoneNumber: String? = null,
        var password: String? = null,
        var name: String? = null,
        var upiId: String? = null
    )
}
