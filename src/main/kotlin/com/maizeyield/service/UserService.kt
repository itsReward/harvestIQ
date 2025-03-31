package com.maizeyield.service

import com.maizeyield.dto.UserResponse
import com.maizeyield.dto.UserUpdateRequest

interface UserService {
    /**
     * Get user by ID
     */
    fun getUserById(id: Long): UserResponse

    /**
     * Get current authenticated user
     */
    fun getCurrentUser(): UserResponse

    /**
     * Update user profile
     */
    fun updateUser(id: Long, request: UserUpdateRequest): UserResponse

    /**
     * Delete user account
     */
    fun deleteUser(id: Long): Boolean
}