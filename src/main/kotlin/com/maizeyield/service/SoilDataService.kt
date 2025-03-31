package com.maizeyield.service

import com.maizeyield.dto.SoilDataCreateRequest
import com.maizeyield.dto.SoilDataResponse
import com.maizeyield.dto.SoilDataUpdateRequest
import java.time.LocalDate

interface SoilDataService {
    /**
     * Get all soil data for a farm
     */
    fun getSoilDataByFarmId(userId: Long, farmId: Long): List<SoilDataResponse>

    /**
     * Get soil data by ID
     */
    fun getSoilDataById(userId: Long, soilDataId: Long): SoilDataResponse

    /**
     * Add soil data to a farm
     */
    fun addSoilData(userId: Long, farmId: Long, request: SoilDataCreateRequest): SoilDataResponse

    /**
     * Update soil data
     */
    fun updateSoilData(userId: Long, soilDataId: Long, request: SoilDataUpdateRequest): SoilDataResponse

    /**
     * Delete soil data
     */
    fun deleteSoilData(userId: Long, soilDataId: Long): Boolean

    /**
     * Get latest soil data for a farm
     */
    fun getLatestSoilData(userId: Long, farmId: Long): SoilDataResponse?

    /**
     * Get soil data for a specific date range
     */
    fun getSoilDataByDateRange(userId: Long, farmId: Long, startDate: LocalDate, endDate: LocalDate): List<SoilDataResponse>
}