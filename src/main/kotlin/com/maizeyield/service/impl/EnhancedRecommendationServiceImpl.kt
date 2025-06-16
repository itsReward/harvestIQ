package com.maizeyield.service.impl

import com.maizeyield.dto.RecommendationResponse
import com.maizeyield.dto.YieldPredictionResponse
import com.maizeyield.exception.ResourceNotFoundException
import com.maizeyield.exception.UnauthorizedAccessException
import com.maizeyield.model.*
import com.maizeyield.repository.*
import com.maizeyield.service.FarmService
import com.maizeyield.service.RecommendationService
import com.maizeyield.service.external.AdvancedRecommendationService
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.math.abs

private val logger = KotlinLogging.logger {}

@Service
class EnhancedRecommendationServiceImpl(
    private val recommendationRepository: RecommendationRepository,
    private val plantingSessionRepository: PlantingSessionRepository,
    private val yieldPredictionRepository: YieldPredictionRepository,
    private val yieldHistoryRepository: YieldHistoryRepository,
    private val weatherDataRepository: WeatherDataRepository,
    private val soilDataRepository: SoilDataRepository,
    private val farmService: FarmService,
    private val advancedRecommendationService: AdvancedRecommendationService
) : RecommendationService {

    // Define threshold constants for triggering recommendations
    companion object {
        const val YIELD_DEFICIT_THRESHOLD_PERCENT = 15.0 // Trigger if prediction is 15% below expected
        const val LOW_CONFIDENCE_THRESHOLD = 65.0 // Trigger if prediction confidence is below 65%
        const val CRITICAL_YIELD_THRESHOLD = 2.0 // tons per hectare - critical low yield
        const val OPTIMAL_YIELD_TARGET = 6.0 // tons per hectare - optimal yield target for Zimbabwe
    }

    @Transactional
    override fun analyzeAndGenerateRecommendations(
        userId: Long,
        plantingSessionId: Long,
        predictionResult: YieldPredictionResponse
    ): List<RecommendationResponse> {
        logger.info { "Analyzing prediction and generating recommendations for session: $plantingSessionId" }

        val session = plantingSessionRepository.findByIdOrNull(plantingSessionId)
            ?: throw ResourceNotFoundException("Planting session not found")

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("Unauthorized access to planting session")
        }

        val recommendations = mutableListOf<RecommendationResponse>()

        // Analyze prediction and determine if intervention is needed
        val analysisResult = analyzePredictionResult(predictionResult, session)

        if (analysisResult.needsIntervention) {
            logger.info { "Prediction below optimum - generating targeted recommendations" }

            // Generate context-specific recommendations based on analysis
            recommendations.addAll(generateContextualRecommendations(session, analysisResult))

            // Generate advanced AI recommendations if needed
            if (analysisResult.criticalityLevel >= 3) {
                recommendations.addAll(generateAdvancedRecommendations(session))
            }
        } else {
            logger.info { "Prediction within acceptable range - generating maintenance recommendations" }
            recommendations.addAll(generateMaintenanceRecommendations(session))
        }

        return recommendations.distinctBy { it.title } // Remove duplicates
    }

    private fun analyzePredictionResult(
        prediction: YieldPredictionResponse,
        session: PlantingSession
    ): PredictionAnalysis {
        val expectedYield = calculateExpectedYield(session)
        val yieldDeficit = expectedYield.toBigDecimal() - prediction.predictedYieldTonsPerHectare
        val deficitPercentage = (yieldDeficit / expectedYield.toBigDecimal()) * 100.0.toBigDecimal()

        val needsIntervention = when {
            prediction.predictedYieldTonsPerHectare < CRITICAL_YIELD_THRESHOLD.toBigDecimal() -> true
            deficitPercentage > YIELD_DEFICIT_THRESHOLD_PERCENT.toBigDecimal() -> true
            prediction.confidencePercentage < LOW_CONFIDENCE_THRESHOLD.toBigDecimal() -> true
            else -> false
        }

        val criticalityLevel = when {
            prediction.predictedYieldTonsPerHectare < CRITICAL_YIELD_THRESHOLD.toBigDecimal() -> 5
            deficitPercentage > 30.toBigDecimal() -> 4
            deficitPercentage > YIELD_DEFICIT_THRESHOLD_PERCENT.toBigDecimal() -> 3
            prediction.confidencePercentage < LOW_CONFIDENCE_THRESHOLD.toBigDecimal() -> 2
            else -> 1
        }

        return PredictionAnalysis(
            expectedYield = expectedYield,
            predictedYield = prediction.predictedYieldTonsPerHectare.toDouble(),
            yieldDeficit = yieldDeficit.toDouble(),
            deficitPercentage = deficitPercentage.toDouble(),
            confidenceScore = prediction.confidencePercentage.toDouble(),
            needsIntervention = needsIntervention,
            criticalityLevel = criticalityLevel,
            riskFactors = identifyRiskFactors(session, prediction)
        )
    }

    private fun calculateExpectedYield(session: PlantingSession): Double {
        // Get historical average for this variety and farm
        val historicalAverage = yieldHistoryRepository.findByFarmAndMaizeVariety(
            session.farm, session.maizeVariety
        ).map { it.yieldTonsPerHectare }.takeIf { it.isNotEmpty() }?.average()

        // Use variety's average yield or optimal target
        return historicalAverage ?: session.maizeVariety.averageYieldTonsPerHectare ?: OPTIMAL_YIELD_TARGET
    }

    private fun identifyRiskFactors(session: PlantingSession, prediction: YieldPredictionResponse): List<String> {
        val riskFactors = mutableListOf<String>()
        val currentDate = LocalDate.now()
        val farm = session.farm

        // Check weather-related risks
        val recentWeather = weatherDataRepository.findByFarmAndDateBetween(
            farm, currentDate.minusDays(7), currentDate
        )

        if (recentWeather.any { it.rainfallMm == null || it.rainfallMm!! < 5.0.toBigDecimal() }) {
            riskFactors.add("DROUGHT_STRESS")
        }

        if (recentWeather.any { it.maxTemperature != null && it.maxTemperature!! > 35.0.toBigDecimal() }) {
            riskFactors.add("HEAT_STRESS")
        }

        if (recentWeather.any { it.rainfallMm != null && it.rainfallMm!! > 50.0.toBigDecimal() }) {
            riskFactors.add("EXCESSIVE_RAINFALL")
        }

        // Check soil-related risks
        val latestSoil = soilDataRepository.findLatestByFarm(farm).orElse(null)
        latestSoil?.let { soil ->
            if (soil.phLevel != null && (soil.phLevel!! < 5.5.toBigDecimal() || soil.phLevel!! > 7.5.toBigDecimal())) {
                riskFactors.add("PH_IMBALANCE")
            }
            if (soil.nitrogenContent != null && soil.nitrogenContent!! < 20.toBigDecimal()) {
                riskFactors.add("NITROGEN_DEFICIENCY")
            }
            if (soil.phosphorusContent != null && soil.phosphorusContent!! < 15.toBigDecimal()) {
                riskFactors.add("PHOSPHORUS_DEFICIENCY")
            }
            if (soil.moistureContent != null && soil.moistureContent!! < 30.toBigDecimal()) {
                riskFactors.add("SOIL_MOISTURE_LOW")
            }
        }

        // Check crop stage risks
        val daysFromPlanting = LocalDate.now().toEpochDay() - session.plantingDate.toEpochDay()
        val variety = session.maizeVariety

        if (daysFromPlanting > variety.maturityDays * 0.8) {
            riskFactors.add("LATE_SEASON_STRESS")
        }

        return riskFactors
    }

    private fun generateContextualRecommendations(
        session: PlantingSession,
        analysis: PredictionAnalysis
    ): List<RecommendationResponse> {
        val recommendations = mutableListOf<Recommendation>()

        // Critical yield recommendations
        if (analysis.predictedYield < CRITICAL_YIELD_THRESHOLD) {
            recommendations.add(createRecommendation(
                session = session,
                category = "EMERGENCY",
                title = "Critical Yield Alert - Immediate Action Required",
                description = "Your predicted yield of ${String.format("%.1f", analysis.predictedYield)} tons/ha is critically low. " +
                        "Expected yield was ${String.format("%.1f", analysis.expectedYield)} tons/ha. Immediate intervention needed.",
                priority = "CRITICAL",
                confidence = 95.0f
            ))
        }

        // Yield deficit recommendations
        if (analysis.deficitPercentage > YIELD_DEFICIT_THRESHOLD_PERCENT) {
            recommendations.add(createRecommendation(
                session = session,
                category = "YIELD_OPTIMIZATION",
                title = "Yield Below Expected - Enhancement Needed",
                description = "Predicted yield is ${String.format("%.1f", analysis.deficitPercentage)}% below expected. " +
                        "Consider implementing yield enhancement strategies.",
                priority = if (analysis.deficitPercentage > 25) "HIGH" else "MEDIUM",
                confidence = 85.0f
            ))
        }

        // Risk factor specific recommendations
        analysis.riskFactors.forEach { riskFactor ->
            recommendations.addAll(generateRiskFactorRecommendations(session, riskFactor))
        }

        // Confidence-based recommendations
        if (analysis.confidenceScore < LOW_CONFIDENCE_THRESHOLD) {
            recommendations.add(createRecommendation(
                session = session,
                category = "DATA_QUALITY",
                title = "Improve Data Quality for Better Predictions",
                description = "Prediction confidence is low (${String.format("%.1f", analysis.confidenceScore)}%). " +
                        "Consider providing more detailed soil and weather data.",
                priority = "MEDIUM",
                confidence = 75.0f
            ))
        }

        return recommendations.map { saveAndMapRecommendation(it) }
    }

    private fun generateRiskFactorRecommendations(
        session: PlantingSession,
        riskFactor: String
    ): List<Recommendation> {
        return when (riskFactor) {
            "DROUGHT_STRESS" -> listOf(
                createRecommendation(
                    session = session,
                    category = "IRRIGATION",
                    title = "Increase Irrigation - Drought Stress Detected",
                    description = "Low rainfall detected in recent days. Increase irrigation frequency to 2-3 times per week. " +
                            "Apply 25-30mm of water per irrigation session.",
                    priority = "HIGH",
                    confidence = 90.0f
                ),
                createRecommendation(
                    session = session,
                    category = "SOIL_MANAGEMENT",
                    title = "Apply Mulching to Retain Soil Moisture",
                    description = "Use organic mulch around plants to reduce water evaporation and maintain soil moisture. " +
                            "Apply 5-7cm thick layer of straw or grass mulch.",
                    priority = "MEDIUM",
                    confidence = 80.0f
                )
            )

            "HEAT_STRESS" -> listOf(
                createRecommendation(
                    session = session,
                    category = "CROP_PROTECTION",
                    title = "Mitigate Heat Stress",
                    description = "High temperatures detected. Increase irrigation frequency and consider shade netting " +
                            "during peak hours (11 AM - 3 PM). Apply foliar spray with potassium.",
                    priority = "HIGH",
                    confidence = 85.0f
                )
            )

            "NITROGEN_DEFICIENCY" -> listOf(
                createRecommendation(
                    session = session,
                    category = "FERTILIZATION",
                    title = "Apply Nitrogen Fertilizer",
                    description = "Soil nitrogen levels are low. Apply 50-75 kg/ha of urea fertilizer. " +
                            "For immediate effect, use liquid nitrogen fertilizer as foliar spray.",
                    priority = "HIGH",
                    confidence = 95.0f
                )
            )

            "PHOSPHORUS_DEFICIENCY" -> listOf(
                createRecommendation(
                    session = session,
                    category = "FERTILIZATION",
                    title = "Supplement Phosphorus",
                    description = "Low phosphorus levels detected. Apply 30-40 kg/ha of DAP fertilizer. " +
                            "Apply near the root zone for better uptake.",
                    priority = "MEDIUM",
                    confidence = 90.0f
                )
            )

            "PH_IMBALANCE" -> listOf(
                createRecommendation(
                    session = session,
                    category = "SOIL_MANAGEMENT",
                    title = "Correct Soil pH",
                    description = "Soil pH is outside optimal range (6.0-7.0). " +
                            if (session.farm.soilData.firstOrNull()?.phLevel?.let { it < 6.0.toBigDecimal() } == true)
                                "Apply agricultural lime at 2-3 tons/ha to raise pH."
                            else "Apply sulfur or organic matter to lower pH.",
                    priority = "MEDIUM",
                    confidence = 85.0f
                )
            )

            "EXCESSIVE_RAINFALL" -> listOf(
                createRecommendation(
                    session = session,
                    category = "DRAINAGE",
                    title = "Improve Field Drainage",
                    description = "Excessive rainfall detected. Ensure proper drainage to prevent waterlogging. " +
                            "Create drainage channels and consider fungicide application to prevent diseases.",
                    priority = "HIGH",
                    confidence = 85.0f
                )
            )

            else -> emptyList()
        }
    }

    private fun generateMaintenanceRecommendations(session: PlantingSession): List<RecommendationResponse> {
        val recommendations = mutableListOf<Recommendation>()

        recommendations.add(createRecommendation(
            session = session,
            category = "MAINTENANCE",
            title = "Continue Current Management Practices",
            description = "Your maize crop is performing well. Continue with current irrigation, fertilization, " +
                    "and pest management practices. Monitor regularly for any changes.",
            priority = "LOW",
            confidence = 80.0f
        ))

        // Add monitoring recommendations
        recommendations.add(createRecommendation(
            session = session,
            category = "MONITORING",
            title = "Regular Crop Monitoring",
            description = "Schedule weekly field inspections to monitor plant health, pest presence, " +
                    "and soil moisture levels. Early detection prevents major issues.",
            priority = "LOW",
            confidence = 85.0f
        ))

        return recommendations.map { saveAndMapRecommendation(it) }
    }

    private fun generateAdvancedRecommendations(session: PlantingSession): List<RecommendationResponse> {
        return try {
            val latestSoil = soilDataRepository.findLatestByFarm(session.farm).orElse(null)
            val recentWeather = weatherDataRepository.findByFarmAndDateBetween(
                session.farm, LocalDate.now().minusDays(7), LocalDate.now()
            )

            advancedRecommendationService.generateAdvancedRecommendations(
                session, recentWeather, latestSoil
            )
        } catch (e: Exception) {
            logger.warn(e) { "Failed to generate advanced recommendations" }
            emptyList()
        }
    }

    private fun createRecommendation(
        session: PlantingSession,
        category: String,
        title: String,
        description: String,
        priority: String,
        confidence: Float
    ): Recommendation {
        return Recommendation(
            plantingSession = session,
            recommendationDate = LocalDate.now(),
            category = category,
            title = title,
            description = description,
            priority = priority,
            confidence = confidence,
            isViewed = false,
            isImplemented = false
        )
    }

    private fun saveAndMapRecommendation(recommendation: Recommendation): RecommendationResponse {
        val saved = recommendationRepository.save(recommendation)
        return RecommendationResponse(
            id = saved.id!!,
            plantingSessionId = saved.plantingSession.id!!,
            category = saved.category,
            title = saved.title,
            description = saved.description,
            priority = saved.priority,
            recommendationDate = saved.recommendationDate,
            isViewed = saved.isViewed,
            isImplemented = saved.isImplemented,
            confidence = saved.confidence
        )
    }

    // Implement existing interface methods
    override fun getRecommendationsByPlantingSession(userId: Long, plantingSessionId: Long): List<RecommendationResponse> {
        val session = plantingSessionRepository.findByIdOrNull(plantingSessionId)
            ?: throw ResourceNotFoundException("Planting session not found")

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("Unauthorized access")
        }

        return recommendationRepository.findByPlantingSessionOrderByRecommendationDateDesc(session)
            .map { saveAndMapRecommendation(it) }
    }

    override fun getRecommendationsByCategory(userId: Long, plantingSessionId: Long, category: String): List<RecommendationResponse> {
        val session = plantingSessionRepository.findByIdOrNull(plantingSessionId)
            ?: throw ResourceNotFoundException("Planting session not found")

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("Unauthorized access")
        }

        return recommendationRepository.findByPlantingSessionAndCategory(session, category)
            .map { saveAndMapRecommendation(it) }
    }

    override fun getRecommendationsByPriority(userId: Long, plantingSessionId: Long, priority: String): List<RecommendationResponse> {
        val session = plantingSessionRepository.findByIdOrNull(plantingSessionId)
            ?: throw ResourceNotFoundException("Planting session not found")

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("Unauthorized access")
        }

        return recommendationRepository.findByPlantingSessionAndPriorityOrderByRecommendationDateDesc(session, priority)
            .map { saveAndMapRecommendation(it) }
    }

    override fun markRecommendationAsViewed(userId: Long, recommendationId: Long): RecommendationResponse {
        val recommendation = recommendationRepository.findByIdOrNull(recommendationId)
            ?: throw ResourceNotFoundException("Recommendation not found")

        if (!farmService.isFarmOwner(userId, recommendation.plantingSession.farm.id!!)) {
            throw UnauthorizedAccessException("Unauthorized access")
        }

        recommendation.isViewed = true
        return saveAndMapRecommendation(recommendation)
    }

    override fun markRecommendationAsImplemented(userId: Long, recommendationId: Long): RecommendationResponse {
        val recommendation = recommendationRepository.findByIdOrNull(recommendationId)
            ?: throw ResourceNotFoundException("Recommendation not found")

        if (!farmService.isFarmOwner(userId, recommendation.plantingSession.farm.id!!)) {
            throw UnauthorizedAccessException("Unauthorized access")
        }

        recommendation.isImplemented = true
        return saveAndMapRecommendation(recommendation)
    }

    override fun generateRecommendations(userId: Long, plantingSessionId: Long): List<RecommendationResponse> {
        val session = plantingSessionRepository.findByIdOrNull(plantingSessionId)
            ?: throw ResourceNotFoundException("Planting session not found")

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("Unauthorized access")
        }

        // Get latest prediction to trigger analysis
        val latestPrediction = yieldPredictionRepository.findLatestByPlantingSession(session)
            .map { prediction ->
                YieldPredictionResponse(
                    id = prediction.id!!,
                    plantingSessionId = session.id!!,
                    predictionDate = prediction.predictionDate,
                    predictedYieldTonsPerHectare = prediction.predictedYieldTonsPerHectare,
                    confidencePercentage = prediction.confidencePercentage,
                    modelVersion = prediction.modelVersion,
                    featuresUsed = prediction.featuresUsed.split(",")
                )
            }
            .orElse(null)

        return if (latestPrediction != null) {
            analyzeAndGenerateRecommendations(userId, plantingSessionId, latestPrediction)
        } else {
            generateMaintenanceRecommendations(session)
        }
    }
}

// Data class for prediction analysis
data class PredictionAnalysis(
    val expectedYield: Double,
    val predictedYield: Double,
    val yieldDeficit: Double,
    val deficitPercentage: Double,
    val confidenceScore: Double,
    val needsIntervention: Boolean,
    val criticalityLevel: Int, // 1-5 scale
    val riskFactors: List<String>
)