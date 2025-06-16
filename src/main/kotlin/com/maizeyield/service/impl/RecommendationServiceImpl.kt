/*
package com.maizeyield.service.impl

import com.maizeyield.dto.RecommendationCreateRequest
import com.maizeyield.dto.RecommendationResponse
import com.maizeyield.dto.RecommendationUpdateRequest
import com.maizeyield.exception.ResourceNotFoundException
import com.maizeyield.exception.UnauthorizedAccessException
import com.maizeyield.model.Recommendation
import com.maizeyield.repository.*
import com.maizeyield.service.FarmService
import com.maizeyield.service.RecommendationService
import com.maizeyield.service.external.AdvancedRecommendationService
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class RecommendationServiceImpl(
    private val recommendationRepository: RecommendationRepository,
    private val plantingSessionRepository: PlantingSessionRepository,
    private val soilDataRepository: SoilDataRepository,
    private val weatherDataRepository: WeatherDataRepository,
    private val yieldPredictionRepository: YieldPredictionRepository,
    private val advancedRecommendationService: AdvancedRecommendationService, // New service injection
    private val farmService: FarmService
) : RecommendationService {

    override fun getRecommendationsByPlantingSessionId(userId: Long, plantingSessionId: Long): List<RecommendationResponse> {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access recommendations for this planting session")
        }

        return recommendationRepository.findByPlantingSession(session).map { recommendation ->
            mapToRecommendationResponse(recommendation)
        }
    }

    override fun getRecommendationById(userId: Long, recommendationId: Long): RecommendationResponse {
        val recommendation = recommendationRepository.findById(recommendationId)
            .orElseThrow { ResourceNotFoundException("Recommendation not found with ID: $recommendationId") }

        if (!farmService.isFarmOwner(userId, recommendation.plantingSession.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access this recommendation")
        }

        return mapToRecommendationResponse(recommendation)
    }

    @Transactional
    override fun createRecommendation(userId: Long, plantingSessionId: Long, request: RecommendationCreateRequest): RecommendationResponse {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to create recommendations for this planting session")
        }

        val recommendation = Recommendation(
            plantingSession = session,
            recommendationDate = request.recommendationDate,
            category = request.category,
            title = request.title,
            description = request.description,
            priority = request.priority
        )

        val savedRecommendation = recommendationRepository.save(recommendation)
        return mapToRecommendationResponse(savedRecommendation)
    }

    @Transactional
    override fun updateRecommendation(userId: Long, recommendationId: Long, request: RecommendationUpdateRequest): RecommendationResponse {
        val recommendation = recommendationRepository.findById(recommendationId)
            .orElseThrow { ResourceNotFoundException("Recommendation not found with ID: $recommendationId") }

        if (!farmService.isFarmOwner(userId, recommendation.plantingSession.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to update this recommendation")
        }

        val updatedRecommendation = recommendation.copy(
            isViewed = request.isViewed ?: recommendation.isViewed,
            isImplemented = request.isImplemented ?: recommendation.isImplemented
        )

        val savedRecommendation = recommendationRepository.save(updatedRecommendation)
        return mapToRecommendationResponse(savedRecommendation)
    }

    @Transactional
    override fun deleteRecommendation(userId: Long, recommendationId: Long): Boolean {
        val recommendation = recommendationRepository.findById(recommendationId)
            .orElseThrow { ResourceNotFoundException("Recommendation not found with ID: $recommendationId") }

        if (!farmService.isFarmOwner(userId, recommendation.plantingSession.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to delete this recommendation")
        }

        recommendationRepository.delete(recommendation)
        return true
    }

    override fun getUnviewedRecommendations(userId: Long, plantingSessionId: Long): List<RecommendationResponse> {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access recommendations for this planting session")
        }

        return recommendationRepository.findByPlantingSessionAndIsViewedFalse(session).map { recommendation ->
            mapToRecommendationResponse(recommendation)
        }
    }

    override fun getUnimplementedRecommendations(userId: Long, plantingSessionId: Long): List<RecommendationResponse> {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access recommendations for this planting session")
        }

        return recommendationRepository.findByPlantingSessionAndIsImplementedFalse(session).map { recommendation ->
            mapToRecommendationResponse(recommendation)
        }
    }

    override fun getRecommendationsByCategory(userId: Long, plantingSessionId: Long, category: String): List<RecommendationResponse> {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access recommendations for this planting session")
        }

        return recommendationRepository.findByPlantingSessionAndCategory(session, category).map { recommendation ->
            mapToRecommendationResponse(recommendation)
        }
    }

    override fun getRecommendationsByPriority(userId: Long, plantingSessionId: Long, priority: String): List<RecommendationResponse> {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access recommendations for this planting session")
        }

        return recommendationRepository.findByPlantingSessionAndPriorityOrderByRecommendationDateDesc(session, priority).map { recommendation ->
            mapToRecommendationResponse(recommendation)
        }
    }

    @Transactional
    override fun generateRecommendations(userId: Long, plantingSessionId: Long): List<RecommendationResponse> {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to generate recommendations for this planting session")
        }

        try {
            logger.info { "Generating advanced recommendations for planting session ID: $plantingSessionId" }

            val farm = session.farm
            val currentDate = LocalDate.now()

            // Get latest soil data
            val latestSoilData = soilDataRepository.findLatestByFarm(farm).orElse(null)

            // Get recent weather data (last 7 days)
            val recentWeatherData = weatherDataRepository.findByFarmAndDateBetween(
                farm,
                currentDate.minusDays(7),
                currentDate
            )

            // Use the advanced recommendation service
            return advancedRecommendationService.generateAdvancedRecommendations(
                session,
                recentWeatherData,
                latestSoilData
            )

        } catch (e: Exception) {
            logger.error(e) { "Error generating recommendations for planting session $plantingSessionId" }
            throw RuntimeException("Failed to generate recommendations", e)
        }
    }

    // Helper method to map Recommendation entity to RecommendationResponse DTO
    private fun mapToRecommendationResponse(recommendation: Recommendation): RecommendationResponse {
        return RecommendationResponse(
            id = recommendation.id!!,
            plantingSessionId = recommendation.plantingSession.id!!,
            category = recommendation.category,
            title = recommendation.title,
            description = recommendation.description,
            priority = recommendation.priority,
            recommendationDate = recommendation.recommendationDate,
            isViewed = recommendation.isViewed,
            isImplemented = recommendation.isImplemented,
            confidence = recommendation.confidence
        )
    }
}*/
