package com.maizeyield.service

import com.maizeyield.dto.RecommendationCreateRequest
import com.maizeyield.dto.RecommendationResponse
import com.maizeyield.dto.RecommendationUpdateRequest

interface RecommendationService {
    /**
     * Get all recommendations for a planting session
     */
    fun getRecommendationsByPlantingSessionId(userId: Long, plantingSessionId: Long): List<RecommendationResponse>

    /**
     * Get recommendation by ID
     */
    fun getRecommendationById(userId: Long, recommendationId: Long): RecommendationResponse

    /**
     * Create a new recommendation
     */
    fun createRecommendation(userId: Long, plantingSessionId: Long, request: RecommendationCreateRequest): RecommendationResponse

    /**
     * Update recommendation status
     */
    fun updateRecommendation(userId: Long, recommendationId: Long, request: RecommendationUpdateRequest): RecommendationResponse

    /**
     * Delete a recommendation
     */
    fun deleteRecommendation(userId: Long, recommendationId: Long): Boolean

    /**
     * Get unviewed recommendations for a planting session
     */
    fun getUnviewedRecommendations(userId: Long, plantingSessionId: Long): List<RecommendationResponse>

    /**
     * Get unimplemented recommendations for a planting session
     */
    fun getUnimplementedRecommendations(userId: Long, plantingSessionId: Long): List<RecommendationResponse>

    /**
     * Get recommendations by category for a planting session
     */
    fun getRecommendationsByCategory(userId: Long, plantingSessionId: Long, category: String): List<RecommendationResponse>

    /**
     * Get recommendations by priority for a planting session
     */
    fun getRecommendationsByPriority(userId: Long, plantingSessionId: Long, priority: String): List<RecommendationResponse>

    /**
     * Generate recommendations for a planting session based on current conditions
     */
    fun generateRecommendations(userId: Long, plantingSessionId: Long): List<RecommendationResponse>
}