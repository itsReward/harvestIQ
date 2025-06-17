package com.maizeyield.service

import com.maizeyield.dto.PlantingSessionCreateRequest
import com.maizeyield.dto.PlantingSessionDetailResponse
import com.maizeyield.dto.PlantingSessionResponse
import com.maizeyield.dto.PlantingSessionUpdateRequest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface PlantingSessionService {
    /**
     * Get all planting sessions for a farm
     */
    fun getPlantingSessionsByFarmId(userId: Long, farmId: Long): List<PlantingSessionResponse>

    /**
     * Get planting session details by ID
     */
    fun getPlantingSessionById(userId: Long, sessionId: Long): PlantingSessionDetailResponse

    /**
     * Create a new planting session
     */
    fun createPlantingSession(userId: Long, farmId: Long, request: PlantingSessionCreateRequest): PlantingSessionResponse

    /**
     * Update planting session details
     */
    fun updatePlantingSession(userId: Long, sessionId: Long, request: PlantingSessionUpdateRequest): PlantingSessionResponse

    /**
     * Delete a planting session
     */
    fun deletePlantingSession(userId: Long, sessionId: Long): Boolean

    /**
     * Get active planting sessions for a farm
     */
    fun getActivePlantingSessions(userId: Long, farmId: Long): List<PlantingSessionResponse>

    /**
     * Get planting sessions for a specific date range
     */
    fun getPlantingSessionsByDateRange(userId: Long, farmId: Long, startDate: LocalDate, endDate: LocalDate): List<PlantingSessionResponse>

    /**
     * Calculate expected harvest date based on maize variety and planting date
     */
    fun calculateExpectedHarvestDate(maizeVarietyId: Long, plantingDate: LocalDate): LocalDate

    /**
     * Check if user has permission to access the planting session
     */
    fun hasAccessToPlantingSession(userId: Long, sessionId: Long): Boolean

    /**
     * Get all planting sessions with pagination
     */
    fun getAllPlantingSessions(userId: Long, pageable: Pageable, search: String): Page<PlantingSessionResponse>

    /**
     * Check if user is owner of planting session
     */
    fun isSessionOwner(userId: Long, sessionId: Long): Boolean

    /**
     * Get planting session status
     */
    fun getPlantingSessionStatus(userId: Long, sessionId: Long): Map<String, Any>

    /**
     * Update planting session status
     */
    fun updatePlantingSessionStatus(userId: Long, sessionId: Long, status: String): Boolean

    /**
     * Get active planting sessions
     */
    fun getActivePlantingSessions(userId: Long): List<PlantingSessionResponse>

    /**
     * Get planting sessions by date range
     */
    fun getPlantingSessionsByDateRange(userId: Long, startDate: LocalDate, endDate: LocalDate): List<PlantingSessionResponse>

}