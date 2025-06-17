package com.maizeyield.service

import com.maizeyield.dto.UserResponse
import com.maizeyield.dto.UserUpdateRequest
import com.maizeyield.model.User

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

    // Dashboard statistics methods
    /**
     * Get total count of all users
     */
    fun getTotalUserCount(): Long

    /**
     * Get count of users with farmer role
     */
    fun getFarmerCount(): Long

    /**
     * Get user growth percentage compared to previous month
     */
    fun getUserGrowthPercentage(): Double

    /**
     * Get farmer growth percentage compared to previous month
     */
    fun getFarmerGrowthPercentage(): Double

    /**
     * Get recent users for activity feed
     */
    fun getRecentUsers(limit: Int): List<User>
}