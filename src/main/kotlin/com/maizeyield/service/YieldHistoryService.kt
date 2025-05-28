package com.maizeyield.service

import com.maizeyield.dto.YieldHistoryCreateRequest
import com.maizeyield.dto.YieldHistoryResponse
import com.maizeyield.dto.YieldHistoryUpdateRequest
import java.math.BigDecimal
import java.time.LocalDate

interface YieldHistoryService {
    /**
     * Get yield history for a planting session
     */
    fun getYieldHistoryByPlantingSessionId(userId: Long, plantingSessionId: Long): List<YieldHistoryResponse>

    /**
     * Get yield history by ID
     */
    fun getYieldHistoryById(userId: Long, yieldHistoryId: Long): YieldHistoryResponse

    /**
     * Add yield history record
     */
    fun addYieldHistory(userId: Long, plantingSessionId: Long, request: YieldHistoryCreateRequest): YieldHistoryResponse

    /**
     * Update yield history record
     */
    fun updateYieldHistory(userId: Long, yieldHistoryId: Long, request: YieldHistoryUpdateRequest): YieldHistoryResponse

    /**
     * Delete yield history record
     */
    fun deleteYieldHistory(userId: Long, yieldHistoryId: Long): Boolean

    /**
     * Get all yield history for a farm
     */
    fun getYieldHistoryByFarmId(userId: Long, farmId: Long): List<YieldHistoryResponse>

    /**
     * Get yield history for a farm within a date range
     */
    fun getYieldHistoryByDateRange(userId: Long, farmId: Long, startDate: LocalDate, endDate: LocalDate): List<YieldHistoryResponse>

    /**
     * Calculate average yield for a farm
     */
    fun calculateAverageYield(userId: Long, farmId: Long): BigDecimal?

    /**
     * Get highest yield record for a farm
     */
    fun getHighestYield(userId: Long, farmId: Long): YieldHistoryResponse?

    /**
     * Get latest yield record for a planting session
     */
    fun getLatestYieldHistory(userId: Long, plantingSessionId: Long): YieldHistoryResponse?
}