package com.maizeyield.repository

import com.maizeyield.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface UserRepository : JpaRepository<User, Long> {

    fun findByUsername(username: String): User?

    fun findByEmail(email: String): User?

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean

    // Fixed: Added alias 'u' after 'User'
    @Query("SELECT u FROM User u ORDER BY u.username")
    fun getAllUsers(): List<User>

    // Methods for getAllUsers with search functionality
    fun findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
        username: String,
        email: String,
        firstName: String,
        lastName: String,
        pageable: Pageable
    ): Page<User>

    // Methods for role-based operations
    fun findByRole(role: String): List<User>

    fun findByRole(role: String, pageable: Pageable): Page<User>

    fun countByRole(role: String): Long

    // Methods for getAllFarmers with search functionality
    fun findByRoleAndUsernameContainingIgnoreCaseOrRoleAndEmailContainingIgnoreCaseOrRoleAndFirstNameContainingIgnoreCaseOrRoleAndLastNameContainingIgnoreCase(
        role1: String, username: String,
        role2: String, email: String,
        role3: String, firstName: String,
        role4: String, lastName: String,
        pageable: Pageable
    ): Page<User>

    // Dashboard query methods
    fun countByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long

    fun countByRoleAndCreatedAtBetween(role: String, startDate: LocalDateTime, endDate: LocalDateTime): Long

    // Fixed: Added alias 'u' after 'User'
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC LIMIT :limit")
    fun findTopByOrderByCreatedAtDesc(@Param("limit") limit: Int): List<User>

    // Alternative method if you need pageable approach
    // Fixed: Added alias 'u' after 'User'
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    fun findRecentUsers(limit: Int = 10): List<User>
}