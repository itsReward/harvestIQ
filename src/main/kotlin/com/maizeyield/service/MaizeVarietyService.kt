package com.maizeyield.service

import com.maizeyield.dto.MaizeVarietyCreateRequest
import com.maizeyield.dto.MaizeVarietyResponse
import com.maizeyield.dto.MaizeVarietyUpdateRequest

interface MaizeVarietyService {
    /**
     * Get all available maize varieties
     */
    fun getAllMaizeVarieties(): List<MaizeVarietyResponse>

    /**
     * Get maize variety by ID
     */
    fun getMaizeVarietyById(varietyId: Long): MaizeVarietyResponse

    /**
     * Create a new maize variety
     */
    fun createMaizeVariety(request: MaizeVarietyCreateRequest): MaizeVarietyResponse

    /**
     * Update maize variety
     */
    fun updateMaizeVariety(varietyId: Long, request: MaizeVarietyUpdateRequest): MaizeVarietyResponse

    /**
     * Delete maize variety
     */
    fun deleteMaizeVariety(varietyId: Long): Boolean

    /**
     * Get drought-resistant varieties
     */
    fun getDroughtResistantVarieties(): List<MaizeVarietyResponse>

    /**
     * Get varieties by maturity days range
     */
    fun getVarietiesByMaturityRange(minDays: Int, maxDays: Int): List<MaizeVarietyResponse>
}