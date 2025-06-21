package com.maizeyield.repository

import com.maizeyield.dto.UserResponse
import com.maizeyield.model.User
import org.springframework.data.domain.Page
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.awt.print.Pageable
import java.time.LocalDateTime

@Repository
interface UserRepository : JpaRepository<User, Long> {


    fun findByUsername(username: String): User?

    fun findByEmail(email: String): User?

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean

    @Query("SELECT u FROM User u ORDER BY u.username")
    fun getAllUsers(): List<User>

    // Dashboard query methods
    fun countByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long

    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC LIMIT :limit")
    fun findTopByOrderByCreatedAtDesc(@Param("limit") limit: Int): List<User>

    // Alternative method if you need pageable approach
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    fun findRecentUsers(limit: Int = 10): List<User>

    // If you have roles or user types, you can add these methods:
    // fun countByRoleAndCreatedAtBetween(role: String, startDate: LocalDateTime, endDate: LocalDateTime): Long
    fun findByRole(role: String): List<User>
}