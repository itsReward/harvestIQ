package com.maizeyield.repository

import com.maizeyield.model.Farm
import com.maizeyield.model.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface FarmRepository : JpaRepository<Farm, Long> {

    // Existing methods
    fun findByUser(user: User): List<Farm>
    fun findByUserAndId(user: User, id: Long): Farm?
    fun countByUser(user: User): Long
    fun findByUserId(userId: Long): List<Farm>

    // Methods for user-specific pagination and search
    fun findByUser(user: User, pageable: Pageable): Page<Farm>

    // Search methods for user's farms
    fun findByUserAndNameContainingIgnoreCaseOrUserAndLocationContainingIgnoreCase(
        user1: User, name: String,
        user2: User, location: String,
        pageable: Pageable
    ): Page<Farm>

    // NEW: Admin search methods for ALL farms in the system
    fun findByNameContainingIgnoreCaseOrLocationContainingIgnoreCase(
        name: String,
        location: String,
        pageable: Pageable
    ): Page<Farm>

    // Region-based methods for getFarmsByRegion
    fun findByLocationContainingIgnoreCase(location: String): List<Farm>

    // Existing statistical queries...
    fun findByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): List<Farm>

    @Query("SELECT COUNT(f) FROM Farm f WHERE f.createdAt >= :sinceDate")
    fun countFarmsCreatedSince(@Param("sinceDate") sinceDate: LocalDateTime): Long

    @Query("SELECT SUM(f.sizeHectares) FROM Farm f")
    fun getTotalFarmArea(): Double?

    @Query("SELECT AVG(f.sizeHectares) FROM Farm f")
    fun getAverageFarmSize(): Double?

    @Query("SELECT f FROM Farm f ORDER BY f.sizeHectares DESC")
    fun findAllOrderBySizeDesc(): List<Farm>

    @Query("SELECT f FROM Farm f ORDER BY f.sizeHectares ASC")
    fun findAllOrderBySizeAsc(): List<Farm>

    @Query("SELECT f FROM Farm f ORDER BY f.createdAt DESC LIMIT :limit")
    fun findRecentFarms(@Param("limit") limit: Int): List<Farm>
}