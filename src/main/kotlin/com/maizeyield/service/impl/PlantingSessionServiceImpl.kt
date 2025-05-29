package com.maizeyield.service.impl

import com.maizeyield.dto.*
import com.maizeyield.exception.ResourceNotFoundException
import com.maizeyield.exception.UnauthorizedAccessException
import com.maizeyield.model.PlantingSession
import com.maizeyield.repository.*
import com.maizeyield.service.FarmService
import com.maizeyield.service.PlantingSessionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class PlantingSessionServiceImpl(
    private val plantingSessionRepository: PlantingSessionRepository,
    private val farmRepository: FarmRepository,
    private val maizeVarietyRepository: MaizeVarietyRepository,
    private val yieldPredictionRepository: YieldPredictionRepository,
    private val recommendationRepository: RecommendationRepository,
    /*private val pestsDiseaseRepository: Repository<*, *>,
    private val farmActivityRepository: Repository<*, *>,*/
    private val weatherDataRepository: WeatherDataRepository,
    private val farmService: FarmService
) : PlantingSessionService {

    override fun getPlantingSessionsByFarmId(userId: Long, farmId: Long): List<PlantingSessionResponse> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return plantingSessionRepository.findByFarm(farm).map { session ->
            mapToPlantingSessionResponse(session)
        }
    }

    override fun getPlantingSessionById(userId: Long, sessionId: Long): PlantingSessionDetailResponse {
        val session = plantingSessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $sessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access this planting session")
        }

        // Get latest yield prediction if available
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

        // Get recommendations
        val recommendations = recommendationRepository.findByPlantingSession(session)
            .map { recommendation ->
                RecommendationResponse(
                    id = recommendation.id!!,
                    plantingSessionId = session.id!!,
                    category = recommendation.category,
                    title = recommendation.title,
                    description = recommendation.description,
                    priority = recommendation.priority,
                    recommendationDate = recommendation.recommendationDate,
                    isViewed = recommendation.isViewed,
                    isImplemented = recommendation.isImplemented,
                    confidence = recommendation.confidence,
                )
            }

        // Calculate days from planting and growth stage
        val daysFromPlanting = LocalDate.now().toEpochDay().minus(session.plantingDate.toEpochDay()).toInt()
        val growthStage = calculateGrowthStage(daysFromPlanting)

        return PlantingSessionDetailResponse(
            id = session.id!!,
            farmId = session.farm.id!!,
            maizeVariety = MaizeVarietyResponse(
                id = session.maizeVariety.id!!,
                name = session.maizeVariety.name,
                maturityDays = session.maizeVariety.maturityDays,
                optimalTemperatureMin = session.maizeVariety.optimalTemperatureMin,
                optimalTemperatureMax = session.maizeVariety.optimalTemperatureMax,
                droughtResistance = session.maizeVariety.droughtResistance,
                diseaseResistance = session.maizeVariety.diseaseResistance,
                description = session.maizeVariety.description
            ),
            plantingDate = session.plantingDate,
            expectedHarvestDate = session.expectedHarvestDate,
            seedRateKgPerHectare = session.seedRateKgPerHectare,
            rowSpacingCm = session.rowSpacingCm,
            fertilizerType = session.fertilizerType,
            fertilizerAmountKgPerHectare = session.fertilizerAmountKgPerHectare,
            irrigationMethod = session.irrigationMethod,
            notes = session.notes,
            daysFromPlanting = daysFromPlanting,
            growthStage = growthStage,
            predictions = if (latestPrediction != null) listOf(latestPrediction) else emptyList(),
            recommendations = recommendations,
            // These would be populated with actual data in a full implementation
            pestsDiseases = emptyList(),
            farmActivities = emptyList(),
            weatherData = emptyList()
        )
    }

    @Transactional
    override fun createPlantingSession(
        userId: Long,
        farmId: Long,
        request: PlantingSessionCreateRequest
    ): PlantingSessionResponse {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to add a planting session to this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        val maizeVariety = maizeVarietyRepository.findById(request.maizeVarietyId)
            .orElseThrow { ResourceNotFoundException("Maize variety not found with ID: ${request.maizeVarietyId}") }

        // Calculate expected harvest date if not provided
        val expectedHarvestDate = request.expectedHarvestDate
            ?: calculateExpectedHarvestDate(request.maizeVarietyId, request.plantingDate)

        val session = PlantingSession(
            farm = farm,
            maizeVariety = maizeVariety,
            plantingDate = request.plantingDate,
            expectedHarvestDate = expectedHarvestDate,
            seedRateKgPerHectare = request.seedRateKgPerHectare,
            rowSpacingCm = request.rowSpacingCm,
            fertilizerType = request.fertilizerType,
            fertilizerAmountKgPerHectare = request.fertilizerAmountKgPerHectare,
            irrigationMethod = request.irrigationMethod,
            notes = request.notes
        )

        val savedSession = plantingSessionRepository.save(session)

        return mapToPlantingSessionResponse(savedSession)
    }

    @Transactional
    override fun updatePlantingSession(
        userId: Long,
        sessionId: Long,
        request: PlantingSessionUpdateRequest
    ): PlantingSessionResponse {
        val session = plantingSessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $sessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to update this planting session")
        }

        val updatedSession = session.copy(
            expectedHarvestDate = request.expectedHarvestDate ?: session.expectedHarvestDate,
            seedRateKgPerHectare = request.seedRateKgPerHectare ?: session.seedRateKgPerHectare,
            rowSpacingCm = request.rowSpacingCm ?: session.rowSpacingCm,
            fertilizerType = request.fertilizerType ?: session.fertilizerType,
            fertilizerAmountKgPerHectare = request.fertilizerAmountKgPerHectare ?: session.fertilizerAmountKgPerHectare,
            irrigationMethod = request.irrigationMethod ?: session.irrigationMethod,
            notes = request.notes ?: session.notes
        )

        val savedSession = plantingSessionRepository.save(updatedSession)

        return mapToPlantingSessionResponse(savedSession)
    }

    @Transactional
    override fun deletePlantingSession(userId: Long, sessionId: Long): Boolean {
        val session = plantingSessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $sessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to delete this planting session")
        }

        plantingSessionRepository.delete(session)
        return true
    }

    override fun getActivePlantingSessions(userId: Long, farmId: Long): List<PlantingSessionResponse> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return plantingSessionRepository.findActiveSessionsByFarm(farm, LocalDate.now()).map { session ->
            mapToPlantingSessionResponse(session)
        }
    }

    override fun getPlantingSessionsByDateRange(
        userId: Long,
        farmId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<PlantingSessionResponse> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return plantingSessionRepository.findByFarmAndPlantingDateBetween(farm, startDate, endDate).map { session ->
            mapToPlantingSessionResponse(session)
        }
    }

    override fun calculateExpectedHarvestDate(maizeVarietyId: Long, plantingDate: LocalDate): LocalDate {
        val maizeVariety = maizeVarietyRepository.findById(maizeVarietyId)
            .orElseThrow { ResourceNotFoundException("Maize variety not found with ID: $maizeVarietyId") }

        // Simple calculation: planting date + maturity days
        return plantingDate.plusDays(maizeVariety.maturityDays.toLong())
    }

    override fun hasAccessToPlantingSession(userId: Long, sessionId: Long): Boolean {
        val session = plantingSessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $sessionId") }

        return farmService.isFarmOwner(userId, session.farm.id!!)
    }

    // Helper method to map PlantingSession entity to PlantingSessionResponse DTO
    private fun mapToPlantingSessionResponse(session: PlantingSession): PlantingSessionResponse {
        // Calculate days from planting
        val daysFromPlanting = LocalDate.now().toEpochDay().minus(session.plantingDate.toEpochDay()).toInt()

        // Get growth stage based on days from planting
        val growthStage = calculateGrowthStage(daysFromPlanting)

        // Get latest prediction if available
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

        return PlantingSessionResponse(
            id = session.id!!,
            farmId = session.farm.id!!,
            maizeVariety = MaizeVarietyResponse(
                id = session.maizeVariety.id!!,
                name = session.maizeVariety.name,
                maturityDays = session.maizeVariety.maturityDays,
                optimalTemperatureMin = session.maizeVariety.optimalTemperatureMin,
                optimalTemperatureMax = session.maizeVariety.optimalTemperatureMax,
                droughtResistance = session.maizeVariety.droughtResistance,
                diseaseResistance = session.maizeVariety.diseaseResistance,
                description = session.maizeVariety.description
            ),
            plantingDate = session.plantingDate,
            expectedHarvestDate = session.expectedHarvestDate,
            seedRateKgPerHectare = session.seedRateKgPerHectare,
            rowSpacingCm = session.rowSpacingCm,
            fertilizerType = session.fertilizerType,
            fertilizerAmountKgPerHectare = session.fertilizerAmountKgPerHectare,
            irrigationMethod = session.irrigationMethod,
            notes = session.notes,
            daysFromPlanting = daysFromPlanting,
            growthStage = growthStage,
            latestPrediction = latestPrediction
        )
    }

    // Helper method to calculate growth stage based on days from planting
    private fun calculateGrowthStage(daysFromPlanting: Int): String {
        return when {
            daysFromPlanting < 0 -> "Not Planted"
            daysFromPlanting < 15 -> "Germination & Emergence (VE)"
            daysFromPlanting < 30 -> "Vegetative Growth (V1-V5)"
            daysFromPlanting < 60 -> "Rapid Growth (V6-V12)"
            daysFromPlanting < 80 -> "Tasseling & Silking (VT-R1)"
            daysFromPlanting < 100 -> "Kernel Development (R2-R4)"
            daysFromPlanting < 120 -> "Maturity (R5-R6)"
            else -> "Ready for Harvest"
        }
    }
}