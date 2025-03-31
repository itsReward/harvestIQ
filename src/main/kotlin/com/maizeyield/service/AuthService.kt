package com.maizeyield.service

import com.maizeyield.dto.LoginResponse
import com.maizeyield.dto.UserLoginRequest
import com.maizeyield.dto.UserRegistrationRequest
import com.maizeyield.dto.UserResponse
import com.maizeyield.dto.PasswordChangeRequest

interface AuthService {
    /**
     * Register a new user
     */
    fun registerUser(request: UserRegistrationRequest): UserResponse

    /**
     * Authenticate a user and generate JWT token
     */
    fun loginUser(request: UserLoginRequest): LoginResponse

    /**
     * Validate JWT token
     */
    fun validateToken(token: String): Boolean

    /**
     * Get user ID from JWT token
     */
    fun getUserIdFromToken(token: String): Long

    /**
     * Change user password
     */
    fun changePassword(userId: Long, request: PasswordChangeRequest): Boolean
}