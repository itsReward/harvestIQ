package com.maizeyield.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

// Request DTOs
data class UserRegistrationRequest(
    val username: String,
    val email: String,
    val password: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null
)

data class UserLoginRequest(
    val username: String,
    val password: String
)

data class UserUpdateRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null
)

data class PasswordChangeRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String
)

// Response DTOs
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val createdAt: LocalDateTime,
    val lastLogin: LocalDateTime? = null,
    val role: String = "FARMER"
)

data class LoginResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val userId: Long,
    val username: String
)