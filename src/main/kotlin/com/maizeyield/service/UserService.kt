package com.maizeyield.service

import com.maizeyield.dto.UserResponse
import com.maizeyield.dto.UserUpdateRequest
import com.maizeyield.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

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

    /**
     * Get all users with pagination and search (Admin only)
     */
    fun getAllUsers(pageable: Pageable, search: String): Page<UserResponse>

    /**
     * Get all farmers with pagination and search (Admin only)
     */
    fun getAllFarmers(pageable: Pageable, search: String): Page<UserResponse>

    /**
     * Update user role (Admin only)
     */
    fun updateUserRole(userId: Long, role: String): UserResponse

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