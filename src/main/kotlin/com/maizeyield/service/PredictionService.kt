package com.maizeyield.service

import com.maizeyield.dto.FactorImportance
import com.maizeyield.dto.PredictionRequest
import com.maizeyield.dto.PredictionResult
import com.maizeyield.dto.YieldPredictionResponse
import com.maizeyield.model.YieldPrediction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

    /**
     * Get all predictions with pagination
     */
    fun getAllPredictions(userId: Long, pageable: Pageable, sessionId: Long?): Iterable<YieldPredictionResponse>

    /**
     * Check if user is owner of prediction
     */
    fun isPredictionOwner(userId: Long, predictionId: Long): Boolean

    /**
     * Delete a prediction
     */
    fun deletePrediction(userId: Long, predictionId: Long): Boolean

    /**
     * Get recent predictions
     */
    fun getRecentPredictions(userId: Long, limit: Int): List<YieldPredictionResponse>

    /**
     * Get prediction accuracy metrics (Admin only)
     */
    fun getPredictionAccuracyMetrics(): Map<String, Any>
}