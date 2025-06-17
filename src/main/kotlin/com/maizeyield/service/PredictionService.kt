package com.maizeyield.service

import com.maizeyield.dto.FactorImportance
import com.maizeyield.dto.PredictionRequest
import com.maizeyield.dto.PredictionResult
import com.maizeyield.dto.YieldPredictionResponse
import com.maizeyield.model.YieldPrediction
import java.time.LocalDate

interface PredictionService {
    /**
     * Generate a yield prediction for a planting session
     */
    fun generatePrediction(userId: Long, plantingSessionId: Long): YieldPredictionResponse

    /**
     * Get all predictions for a planting session
     */
    fun getPredictionsByPlantingSessionId(userId: Long, plantingSessionId: Long): List<YieldPredictionResponse>

    /**
     * Get prediction by ID
     */
    fun getPredictionById(userId: Long, predictionId: Long): YieldPredictionResponse

    /**
     * Get latest prediction for a planting session
     */
    fun getLatestPrediction(userId: Long, plantingSessionId: Long): YieldPredictionResponse?

    /**
     * Execute prediction model with provided data
     */
    fun executePredictionModel(request: PredictionRequest): PredictionResult

    /**
     * Get predictions for all active planting sessions of a farm
     */
    fun getPredictionsForFarm(userId: Long, farmId: Long): List<YieldPredictionResponse>

    /**
     * Generate predictions for all active planting sessions of a farm
     */
    fun generatePredictionsForFarm(userId: Long, farmId: Long): List<YieldPredictionResponse>

    /**
     * Get the important factors affecting yield for a specific prediction
     */
    fun getImportantFactors(userId: Long, predictionId: Long): List<FactorImportance>

    /**
     * Get prediction history for a planting session
     */
    fun getPredictionHistory(userId: Long, plantingSessionId: Long, startDate: LocalDate, endDate: LocalDate): List<YieldPredictionResponse>

    // Dashboard statistics methods
    /**
     * Get total count of all predictions
     */
    fun getTotalPredictionCount(): Long

    /**
     * Get count of active planting sessions (last 30 days)
     */
    fun getActiveSessionCount(): Long

    /**
     * Get average yield from recent predictions
     */
    fun getAverageYield(): Double

    /**
     * Get session growth percentage compared to previous month
     */
    fun getSessionGrowthPercentage(): Double

    /**
     * Get prediction growth percentage compared to previous month
     */
    fun getPredictionGrowthPercentage(): Double

    /**
     * Get yield growth percentage compared to previous month
     */
    fun getYieldGrowthPercentage(): Double

    /**
     * Get recent predictions for activity feed
     */
    fun getRecentPredictions(limit: Int): List<YieldPrediction>
}