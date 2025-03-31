package com.maizeyield.service.impl

import com.maizeyield.dto.FactorImportance
import com.maizeyield.dto.PredictionRequest
import com.maizeyield.dto.PredictionResult
import com.maizeyield.dto.YieldPredictionResponse
import com.maizeyield.exception.PredictionException
import com.maizeyield.exception.ResourceNotFoundException
import com.maizeyield.exception.UnauthorizedAccessException
import com.maizeyield.model.PlantingSession
import com.maizeyield.model.YieldPrediction
import com.maizeyield.repository.*
import com.maizeyield.service.FarmService
import com.maizeyield.service.PlantingSessionService
import com.maizeyield.service.PredictionService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.*
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

@Service
class PredictionServiceImpl(
    private val yieldPredictionRepository: YieldPredictionRepository,
    private val plantingSessionRepository: PlantingSessionRepository,
    private val farmRepository: FarmRepository,
    private val soilDataRepository: SoilDataRepository,
    private val weatherDataRepository: WeatherDataRepository,
    private val plantingSessionService: PlantingSessionService,
    private val farmService: FarmService
) : PredictionService {

    @Value("\${prediction.model.version:1.0.0}")
    private lateinit var modelVersion: String

    private val random = Random(1234) // Fixed seed for reproducibility

    override fun generatePrediction(userId: Long, plantingSessionId: Long): YieldPredictionResponse {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to generate predictions for this planting session")
        }

        try {
            // Prepare prediction request
            val request = PredictionRequest(
                farmId = session.farm.id!!,
                plantingSessionId = session.id!!,
                maizeVarietyId = session.maizeVariety.id!!,
                plantingDate = session.plantingDate,
                currentDate = LocalDate.now(),
                includeSoilData = true,
                includeWeatherData = true,
                includeHistoricalYields = true
            )

            // Execute prediction model
            val predictionResult = executePredictionModel(request)

            // Save prediction to database
            val features = predictionResult.featuresUsed.joinToString(",")

            val prediction = YieldPrediction(
                plantingSession = session,
                predictionDate = predictionResult.predictionDate,
                predictedYieldTonsPerHectare = predictionResult.predictedYieldTonsPerHectare,
                confidencePercentage = predictionResult.confidencePercentage,
                modelVersion = predictionResult.modelVersion,
                featuresUsed = features
            )

            val savedPrediction = yieldPredictionRepository.save(prediction)

            // Map to response DTO
            return YieldPredictionResponse(
                id = savedPrediction.id!!,
                plantingSessionId = plantingSessionId,
                predictionDate = savedPrediction.predictionDate,
                predictedYieldTonsPerHectare = savedPrediction.predictedYieldTonsPerHectare,
                confidencePercentage = savedPrediction.confidencePercentage,
                modelVersion = savedPrediction.modelVersion,
                featuresUsed = savedPrediction.featuresUsed.split(","),
                predictionQuality = getPredictionQuality(savedPrediction.confidencePercentage)
            )
        } catch (e: Exception) {
            logger.error(e) { "Error generating yield prediction" }
            throw PredictionException("Failed to generate yield prediction", e.message)
        }
    }

    override fun getPredictionsByPlantingSessionId(userId: Long, plantingSessionId: Long): List<YieldPredictionResponse> {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access predictions for this planting session")
        }

        return yieldPredictionRepository.findByPlantingSession(session).map { prediction ->
            mapToPredictionResponse(prediction)
        }
    }

    override fun getPredictionById(userId: Long, predictionId: Long): YieldPredictionResponse {
        val prediction = yieldPredictionRepository.findById(predictionId)
            .orElseThrow { ResourceNotFoundException("Prediction not found with ID: $predictionId") }

        if (!farmService.isFarmOwner(userId, prediction.plantingSession.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access this prediction")
        }

        return mapToPredictionResponse(prediction)
    }

    override fun getLatestPrediction(userId: Long, plantingSessionId: Long): YieldPredictionResponse? {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access predictions for this planting session")
        }

        return yieldPredictionRepository.findLatestByPlantingSession(session)
            .map { mapToPredictionResponse(it) }
            .orElse(null)
    }

    override fun executePredictionModel(request: PredictionRequest): PredictionResult {
        // In a production environment, this would call a real ML model
        // For this implementation, we'll create a simplified prediction model

        try {
            logger.info { "Executing prediction model for planting session ID: ${request.plantingSessionId}" }

            val session = plantingSessionRepository.findById(request.plantingSessionId)
                .orElseThrow { ResourceNotFoundException("Planting session not found with ID: ${request.plantingSessionId}") }

            val farm = session.farm
            val maizeVariety = session.maizeVariety

            // Get soil data
            val soilData = if (request.includeSoilData) {
                soilDataRepository.findLatestByFarm(farm).orElse(null)
            } else null

            // Get weather data for the last 30 days
            val today = request.currentDate
            val oneMonthAgo = today.minusMonths(1)
            val weatherData = if (request.includeWeatherData) {
                weatherDataRepository.findByFarmAndDateBetween(farm, oneMonthAgo, today)
            } else emptyList()

            // Calculate days since planting
            val daysSincePlanting = today.toEpochDay() - session.plantingDate.toEpochDay()

            // Base yield for this maize variety (simulated)
            val baseYield = BigDecimal(4.5 + random.nextDouble() * 1.5)

            // Apply soil quality factor
            val soilFactor = if (soilData != null) {
                // Simplified soil quality factor based on soil data
                val phFactor = if (soilData.phLevel != null) {
                    val optimal = 6.5
                    val actual = soilData.phLevel!!.toDouble()
                    val deviation = Math.abs(actual - optimal)
                    max(0.7, 1.0 - deviation * 0.1)
                } else 0.9

                val organicMatterFactor = if (soilData.organicMatterPercentage != null) {
                    min(1.2, 0.8 + soilData.organicMatterPercentage!!.toDouble() * 0.1)
                } else 1.0

                val nutrientFactor = if (soilData.nitrogenContent != null && soilData.phosphorusContent != null) {
                    min(1.3, 0.7 + soilData.nitrogenContent!!.toDouble() * 0.15 + soilData.phosphorusContent!!.toDouble() * 0.1)
                } else 1.0

                (phFactor * organicMatterFactor * nutrientFactor)
            } else 1.0

            // Apply weather factor
            val weatherFactor = if (weatherData.isNotEmpty()) {
                // Simplified weather factor based on rainfall and temperature
                val avgRainfall = weatherData.mapNotNull { it.rainfallMm?.toDouble() }.average().let { if (it.isNaN()) 5.0 else it }
                val avgTemperature = weatherData.mapNotNull { it.averageTemperature?.toDouble() }.average().let { if (it.isNaN()) 25.0 else it }

                // Optimal rainfall is 5-15mm per day, optimal temperature is 20-30C
                val rainfallFactor = when {
                    avgRainfall < 1.0 -> 0.6 // Too dry
                    avgRainfall < 5.0 -> 0.8 + (avgRainfall - 1.0) * 0.05 // Somewhat dry
                    avgRainfall <= 15.0 -> 1.0 // Optimal
                    avgRainfall <= 25.0 -> 1.0 - (avgRainfall - 15.0) * 0.02 // Somewhat wet
                    else -> 0.8 // Too wet
                }

                val temperatureFactor = when {
                    avgTemperature < 15.0 -> 0.7 // Too cold
                    avgTemperature < 20.0 -> 0.7 + (avgTemperature - 15.0) * 0.06 // Somewhat cold
                    avgTemperature <= 30.0 -> 1.0 // Optimal
                    avgTemperature <= 35.0 -> 1.0 - (avgTemperature - 30.0) * 0.06 // Somewhat hot
                    else -> 0.7 // Too hot
                }

                rainfallFactor * temperatureFactor
            } else 1.0

            // Apply growth stage factor based on days since planting
            val growthStageFactor = when {
                daysSincePlanting < 0 -> 0.0 // Not planted yet
                daysSincePlanting < 30 -> 0.3 // Early growth
                daysSincePlanting < 60 -> 0.6 // Vegetative growth
                daysSincePlanting < 90 -> 0.8 // Tasseling & Silking
                daysSincePlanting < 120 -> 1.0 // Kernel development
                else -> 1.0 // Maturity
            }

            // Apply maize variety factor
            val varietyFactor = if (maizeVariety.droughtResistance) 1.1 else 1.0

            // Calculate predicted yield
            val predictedYield = baseYield.toDouble() * soilFactor * weatherFactor * growthStageFactor * varietyFactor

            // Calculate confidence level based on available data
            val dataCompleteness = calculateDataCompleteness(soilData != null, weatherData.isNotEmpty())
            val confidencePercentage = BigDecimal(70.0 + dataCompleteness * 20.0 + random.nextDouble() * 5.0)
                .setScale(2, RoundingMode.HALF_UP)

            // Prepare important factors
            val importantFactors = mutableListOf<FactorImportance>()

            // Soil factor
            if (soilData != null) {
                importantFactors.add(
                    FactorImportance(
                        factor = "Soil Quality",
                        importance = BigDecimal(soilFactor * 0.3).setScale(2, RoundingMode.HALF_UP),
                        impact = if (soilFactor >= 1.0) "POSITIVE" else "NEGATIVE"
                    )
                )
            }

            // Weather factor
            if (weatherData.isNotEmpty()) {
                importantFactors.add(
                    FactorImportance(
                        factor = "Weather Conditions",
                        importance = BigDecimal(weatherFactor * 0.4).setScale(2, RoundingMode.HALF_UP),
                        impact = if (weatherFactor >= 1.0) "POSITIVE" else "NEGATIVE"
                    )
                )
            }

            // Growth stage factor
            importantFactors.add(
                FactorImportance(
                    factor = "Growth Stage",
                    importance = BigDecimal(growthStageFactor * 0.2).setScale(2, RoundingMode.HALF_UP),
                    impact = "NEUTRAL"
                )
            )

            // Maize variety factor
            importantFactors.add(
                FactorImportance(
                    factor = "Maize Variety",
                    importance = BigDecimal(varietyFactor * 0.1).setScale(2, RoundingMode.HALF_UP),
                    impact = if (varietyFactor > 1.0) "POSITIVE" else "NEUTRAL"
                )
            )

            // Prepare the list of features used
            val featuresUsed = mutableListOf(
                "PlantingDate",
                "DaysSincePlanting",
                "MaizeVariety"
            )

            if (soilData != null) featuresUsed.add("SoilData")
            if (weatherData.isNotEmpty()) featuresUsed.add("WeatherData")

            return PredictionResult(
                predictedYieldTonsPerHectare = BigDecimal(predictedYield).setScale(2, RoundingMode.HALF_UP),
                confidencePercentage = confidencePercentage,
                predictionDate = LocalDate.now(),
                modelVersion = modelVersion,
                featuresUsed = featuresUsed,
                importantFactors = importantFactors
            )
        } catch (e: Exception) {
            logger.error(e) { "Error executing prediction model" }
            throw PredictionException("Failed to execute prediction model", e.message)
        }
    }

    override fun getPredictionsForFarm(userId: Long, farmId: Long): List<YieldPredictionResponse> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access predictions for this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        // Get active planting sessions
        val activeSessions = plantingSessionRepository.findActiveSessionsByFarm(farm, LocalDate.now())

        // Get latest prediction for each active session
        return activeSessions.mapNotNull { session ->
            yieldPredictionRepository.findLatestByPlantingSession(session)
                .map { mapToPredictionResponse(it) }
                .orElse(null)
        }
    }

    @Transactional
    override fun generatePredictionsForFarm(userId: Long, farmId: Long): List<YieldPredictionResponse> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to generate predictions for this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        // Get active planting sessions
        val activeSessions = plantingSessionRepository.findActiveSessionsByFarm(farm, LocalDate.now())

        // Generate predictions for each active session
        return activeSessions.map { session ->
            generatePrediction(userId, session.id!!)
        }
    }

    override fun getImportantFactors(userId: Long, predictionId: Long): List<FactorImportance> {
        val prediction = yieldPredictionRepository.findById(predictionId)
            .orElseThrow { ResourceNotFoundException("Prediction not found with ID: $predictionId") }

        if (!farmService.isFarmOwner(userId, prediction.plantingSession.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access this prediction")
        }

        // In a production system, this would retrieve factor importance from the model
        // For this implementation, we'll create simulated important factors
        return listOf(
            FactorImportance(
                factor = "Soil Quality",
                importance = BigDecimal("0.30"),
                impact = "POSITIVE"
            ),
            FactorImportance(
                factor = "Weather Conditions",
                importance = BigDecimal("0.40"),
                impact = "NEGATIVE"
            ),
            FactorImportance(
                factor = "Growth Stage",
                importance = BigDecimal("0.20"),
                impact = "NEUTRAL"
            ),
            FactorImportance(
                factor = "Maize Variety",
                importance = BigDecimal("0.10"),
                impact = "POSITIVE"
            )
        )
    }

    override fun getPredictionHistory(
        userId: Long,
        plantingSessionId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<YieldPredictionResponse> {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access predictions for this planting session")
        }

        // Get predictions for the specified date range
        return yieldPredictionRepository.findByPlantingSession(session)
            .filter { it.predictionDate in startDate..endDate }
            .map { mapToPredictionResponse(it) }
    }

    // Helper method to map YieldPrediction entity to YieldPredictionResponse DTO
    private fun mapToPredictionResponse(prediction: YieldPrediction): YieldPredictionResponse {
        return YieldPredictionResponse(
            id = prediction.id!!,
            plantingSessionId = prediction.plantingSession.id!!,
            predictionDate = prediction.predictionDate,
            predictedYieldTonsPerHectare = prediction.predictedYieldTonsPerHectare,
            confidencePercentage = prediction.confidencePercentage,
            modelVersion = prediction.modelVersion,
            featuresUsed = prediction.featuresUsed.split(","),
            predictionQuality = getPredictionQuality(prediction.confidencePercentage)
        )
    }

    // Helper method to determine prediction quality based on confidence percentage
    private fun getPredictionQuality(confidencePercentage: BigDecimal): String {
        return when {
            confidencePercentage.toDouble() >= 90.0 -> "HIGH"
            confidencePercentage.toDouble() >= 70.0 -> "MEDIUM"
            else -> "LOW"
        }
    }

    // Helper method to calculate data completeness
    private fun calculateDataCompleteness(hasSoilData: Boolean, hasWeatherData: Boolean): Double {
        var completeness = 0.0

        if (hasSoilData) completeness += 0.5
        if (hasWeatherData) completeness += 0.5

        return completeness
    }
}