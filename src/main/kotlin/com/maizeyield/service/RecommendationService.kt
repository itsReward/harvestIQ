package com.maizeyield.service

import com.maizeyield.dto.RecommendationResponse
import com.maizeyield.dto.YieldPredictionResponse

interface RecommendationService {

    /**
     * Core method: Analyzes yield prediction and generates contextual recommendations
     * when prediction is below optimum or expected levels
     */
    fun analyzeAndGenerateRecommendations(
        userId: Long,
        plantingSessionId: Long,
        predictionResult: YieldPredictionResponse
    ): List<RecommendationResponse>

    /**
     * Get all recommendations for a planting session
     */
    fun getRecommendationsByPlantingSession(
        userId: Long,
        plantingSessionId: Long
    ): List<RecommendationResponse>

    /**
     * Get recommendations by category (e.g., IRRIGATION, FERTILIZATION, PEST_CONTROL)
     */
    fun getRecommendationsByCategory(
        userId: Long,
        plantingSessionId: Long,
        category: String
    ): List<RecommendationResponse>

    /**
     * Get recommendations by priority (CRITICAL, HIGH, MEDIUM, LOW)
     */
    fun getRecommendationsByPriority(
        userId: Long,
        plantingSessionId: Long,
        priority: String
    ): List<RecommendationResponse>

    /**
     * Mark a recommendation as viewed by the farmer
     */
    fun markRecommendationAsViewed(
        userId: Long,
        recommendationId: Long
    ): RecommendationResponse

    /**
     * Mark a recommendation as implemented by the farmer
     */
    fun markRecommendationAsImplemented(
        userId: Long,
        recommendationId: Long
    ): RecommendationResponse

    /**
     * Generate general recommendations for a planting session
     * (fallback method when no specific prediction analysis is needed)
     */
    fun generateRecommendations(
        userId: Long,
        plantingSessionId: Long
    ): List<RecommendationResponse>
}