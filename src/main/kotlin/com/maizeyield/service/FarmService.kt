package com.maizeyield.service

import com.maizeyield.dto.FarmCreateRequest
import com.maizeyield.dto.FarmDetailResponse
import com.maizeyield.dto.FarmResponse
import com.maizeyield.dto.FarmUpdateRequest

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
}