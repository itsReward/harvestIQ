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
            logger.info { "Generating recommendations for planting session ID: $plantingSessionId" }

            val farm = session.farm
            val currentDate = LocalDate.now()
            val daysSincePlanting = currentDate.toEpochDay() - session.plantingDate.toEpochDay()

            // Get latest soil data
            val latestSoilData = soilDataRepository.findLatestByFarm(farm).orElse(null)

            // Get recent weather data (last 7 days)
            val recentWeatherData = weatherDataRepository.findByFarmAndDateBetween(
                farm,
                currentDate.minusDays(7),
                currentDate
            )

            // Get latest prediction
            val latestPrediction = yieldPredictionRepository.findLatestByPlantingSession(session).orElse(null)

            val recommendations = mutableListOf<Recommendation>()

            // Generate irrigation recommendations based on weather data
            if (recentWeatherData.isNotEmpty()) {
                val avgRainfall = recentWeatherData.mapNotNull { it.rainfallMm?.toDouble() }.average()

                if (!avgRainfall.isNaN() && avgRainfall < 5.0) {
                    recommendations.add(
                        Recommendation(
                            plantingSession = session,
                            recommendationDate = currentDate,
                            category = "IRRIGATION",
                            title = "Increase Irrigation",
                            description = "Recent rainfall has been below optimal levels (${String.format("%.1f", avgRainfall)}mm/day average). Consider increasing irrigation frequency to maintain soil moisture for optimal crop growth.",
                            priority = "HIGH"
                        )
                    )
                } else if (!avgRainfall.isNaN() && avgRainfall > 20.0) {
                    recommendations.add(
                        Recommendation(
                            plantingSession = session,
                            recommendationDate = currentDate,
                            category = "DRAINAGE",
                            title = "Improve Drainage",
                            description = "Recent rainfall has been excessive (${String.format("%.1f", avgRainfall)}mm/day average). Ensure proper drainage to prevent waterlogging and root rot.",
                            priority = "MEDIUM"
                        )
                    )
                }

                // Temperature-based recommendations
                val avgTemperature = recentWeatherData.mapNotNull { it.averageTemperature?.toDouble() }.average()
                if (!avgTemperature.isNaN() && avgTemperature > 35.0) {
                    recommendations.add(
                        Recommendation(
                            plantingSession = session,
                            recommendationDate = currentDate,
                            category = "HEAT_STRESS",
                            title = "Heat Stress Management",
                            description = "Recent temperatures have been high (${String.format("%.1f", avgTemperature)}°C average). Consider additional irrigation and mulching to reduce heat stress on crops.",
                            priority = "HIGH"
                        )
                    )
                }
            }

            // Generate fertilizer recommendations based on soil data
            if (latestSoilData != null) {
                // Nitrogen recommendations
                latestSoilData.nitrogenContent?.let { nitrogen ->
                    if (nitrogen.toDouble() < 1.5) {
                        recommendations.add(
                            Recommendation(
                                plantingSession = session,
                                recommendationDate = currentDate,
                                category = "FERTILIZATION",
                                title = "Nitrogen Fertilizer Application",
                                description = "Soil nitrogen content is low (${nitrogen}%). Apply nitrogen-rich fertilizer such as urea at 200-250 kg/ha to support vegetative growth.",
                                priority = "HIGH"
                            )
                        )
                    }
                }

                // Phosphorus recommendations
                latestSoilData.phosphorusContent?.let { phosphorus ->
                    if (phosphorus.toDouble() < 1.2) {
                        recommendations.add(
                            Recommendation(
                                plantingSession = session,
                                recommendationDate = currentDate,
                                category = "FERTILIZATION",
                                title = "Phosphorus Fertilizer Application",
                                description = "Soil phosphorus content is low (${phosphorus}%). Apply phosphorus-rich fertilizer such as DAP at 100-150 kg/ha to support root development.",
                                priority = "MEDIUM"
                            )
                        )
                    }
                }

                // pH recommendations
                latestSoilData.phLevel?.let { ph ->
                    when (ph.toDouble()) {
                        in 0.0..5.5 -> recommendations.add(
                            Recommendation(
                                plantingSession = session,
                                recommendationDate = currentDate,
                                category = "SOIL_MANAGEMENT",
                                title = "Soil pH Adjustment",
                                description = "Soil pH is acidic (${ph}). Apply agricultural lime at 2-4 tonnes/ha to raise pH to optimal range (6.0-7.0) for maize cultivation.",
                                priority = "MEDIUM"
                            )
                        )
                        in 7.5..14.0 -> recommendations.add(
                            Recommendation(
                                plantingSession = session,
                                recommendationDate = currentDate,
                                category = "SOIL_MANAGEMENT",
                                title = "Soil pH Adjustment",
                                description = "Soil pH is alkaline (${ph}). Apply organic matter or sulfur to lower pH to optimal range (6.0-7.0) for maize cultivation.",
                                priority = "MEDIUM"
                            )
                        )
                    }
                }
            }

            // Growth stage-based recommendations
            when (daysSincePlanting.toInt()) {
                in 0..14 -> {
                    recommendations.add(
                        Recommendation(
                            plantingSession = session,
                            recommendationDate = currentDate,
                            category = "CROP_MANAGEMENT",
                            title = "Early Growth Monitoring",
                            description = "Monitor seedling emergence and check for pest damage. Ensure adequate soil moisture for germination.",
                            priority = "MEDIUM"
                        )
                    )
                }
                in 15..30 -> {
                    recommendations.add(
                        Recommendation(
                            plantingSession = session,
                            recommendationDate = currentDate,
                            category = "WEED_CONTROL",
                            title = "Weed Management",
                            description = "Apply pre-emergence herbicide or conduct manual weeding to control weeds during early vegetative growth stage.",
                            priority = "HIGH"
                        )
                    )
                }
                in 31..60 -> {
                    recommendations.add(
                        Recommendation(
                            plantingSession = session,
                            recommendationDate = currentDate,
                            category = "FERTILIZATION",
                            title = "Side-dress Nitrogen Application",
                            description = "Apply additional nitrogen fertilizer as side-dressing during rapid growth phase to support vigorous vegetative development.",
                            priority = "HIGH"
                        )
                    )
                }
                in 61..80 -> {
                    recommendations.add(
                        Recommendation(
                            plantingSession = session,
                            recommendationDate = currentDate,
                            category = "PEST_CONTROL",
                            title = "Monitor for Pests",
                            description = "Regularly inspect plants for common pests like fall armyworm and stem borers during tasseling stage. Apply appropriate pesticides if needed.",
                            priority = "HIGH"
                        )
                    )
                }
                in 81..100 -> {
                    recommendations.add(
                        Recommendation(
                            plantingSession = session,
                            recommendationDate = currentDate,
                            category = "DISEASE_CONTROL",
                            title = "Disease Prevention",
                            description = "Monitor for fungal diseases during kernel development. Ensure good air circulation and avoid overhead irrigation.",
                            priority = "MEDIUM"
                        )
                    )
                }
                in 101..120 -> {
                    recommendations.add(
                        Recommendation(
                            plantingSession = session,
                            recommendationDate = currentDate,
                            category = "HARVEST_PREP",
                            title = "Harvest Preparation",
                            description = "Begin monitoring kernel moisture content. Prepare for harvest when moisture drops to 20-25%.",
                            priority = "MEDIUM"
                        )
                    )
                }
                else -> {
                    if (daysSincePlanting > 120) {
                        recommendations.add(
                            Recommendation(
                                plantingSession = session,
                                recommendationDate = currentDate,
                                category = "HARVEST",
                                title = "Ready for Harvest",
                                description = "Crop should be ready for harvest. Check kernel moisture and harvest when conditions are optimal.",
                                priority = "HIGH"
                            )
                        )
                    }
                }
            }

            // Prediction-based recommendations
            latestPrediction?.let { prediction ->
                if (prediction.confidencePercentage.toDouble() < 70.0) {
                    recommendations.add(
                        Recommendation(
                            plantingSession = session,
                            recommendationDate = currentDate,
                            category = "DATA_COLLECTION",
                            title = "Improve Data Collection",
                            description = "Prediction confidence is low (${prediction.confidencePercentage}%). Consider collecting more soil and weather data to improve yield predictions.",
                            priority = "LOW"
                        )
                    )
                }

                if (prediction.predictedYieldTonsPerHectare.toDouble() < 3.0) {
                    recommendations.add(
                        Recommendation(
                            plantingSession = session,
                            recommendationDate = currentDate,
                            category = "YIELD_IMPROVEMENT",
                            title = "Yield Enhancement Strategies",
                            description = "Predicted yield is below average (${prediction.predictedYieldTonsPerHectare} tonnes/ha). Consider implementing intensive management practices including optimal fertilization and pest control.",
                            priority = "HIGH"
                        )
                    )
                }
            }

            // General recommendations based on variety
            if (session.maizeVariety.droughtResistance && recentWeatherData.isNotEmpty()) {
                val avgRainfall = recentWeatherData.mapNotNull { it.rainfallMm?.toDouble() }.average()
                if (!avgRainfall.isNaN() && avgRainfall < 2.0) {
                    recommendations.add(
                        Recommendation(
                            plantingSession = session,
                            recommendationDate = currentDate,
                            category = "VARIETY_MANAGEMENT",
                            title = "Leverage Drought Resistance",
                            description = "Your drought-resistant variety can tolerate low rainfall, but ensure minimal irrigation during critical growth stages for optimal yield.",
                            priority = "LOW"
                        )
                    )
                }
            }

            // Save all recommendations
            val savedRecommendations = if (recommendations.isNotEmpty()) {
                recommendationRepository.saveAll(recommendations)
            } else {
                // If no specific recommendations are generated, create a general one
                val generalRecommendation = Recommendation(
                    plantingSession = session,
                    recommendationDate = currentDate,
                    category = "GENERAL",
                    title = "Continue Regular Monitoring",
                    description = "Your maize crop appears to be in good condition. Continue regular monitoring and maintain current management practices.",
                    priority = "LOW"
                )
                listOf(recommendationRepository.save(generalRecommendation))
            }

            return savedRecommendations.map { mapToRecommendationResponse(it) }

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
            isImplemented = recommendation.isImplemented
        )
    }
}