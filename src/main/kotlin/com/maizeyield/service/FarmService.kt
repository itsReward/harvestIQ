package com.maizeyield.service

import com.maizeyield.dto.FarmCreateRequest
import com.maizeyield.dto.FarmDetailResponse
import com.maizeyield.dto.FarmResponse
import com.maizeyield.dto.FarmUpdateRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface FarmService {
    /**
     * Get all farms for a user
     */
    fun getFarmsByUserId(userId: Long): List<FarmResponse>

    /**
     * Get farm details by ID
     */
    fun getFarmById(userId: Long, farmId: Long): FarmDetailResponse

    /**
     * Create a new farm
     */
    fun createFarm(userId: Long, request: FarmCreateRequest): FarmResponse

    /**
     * Update farm details
     */
    fun updateFarm(userId: Long, farmId: Long, request: FarmUpdateRequest): FarmResponse

    /**
     * Delete a farm
     */
    fun deleteFarm(userId: Long, farmId: Long): Boolean

    /**
     * Check if user owns the farm
     */
    fun isFarmOwner(userId: Long, farmId: Long): Boolean

    /**
     * Checks total number of farms
     */
    fun getTotalFarmCount(): Long

    /**
     * Get all farms with pagination and search (for regular users - only their farms)
     */
    fun getAllFarms(userId: Long, pageable: Pageable, search: String): Page<FarmResponse>

    /**
     * Get ALL farms in the system (Admin only)
     */
    fun getAllFarmsForAdmin(pageable: Pageable, search: String): Page<FarmResponse>

    /**
     * Get farm statistics for a specific farm
     */
    fun getFarmStatistics(userId: Long, farmId: Long): Map<String, Any>

    /**
     * Transfer farm ownership
     */
    fun transferFarmOwnership(currentUserId: Long, farmId: Long, newOwnerId: Long): Boolean

    /**
     * Get farms by region (Admin only)
     */
    fun getFarmsByRegion(region: String): List<FarmResponse>

    /**
     * Get farm statistics summary (Admin only)
     */
    fun getFarmStatisticsSummary(): Map<String, Any>
}