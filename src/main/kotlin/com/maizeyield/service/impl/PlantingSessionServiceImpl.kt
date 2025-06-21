package com.maizeyield.service.impl

import com.maizeyield.dto.*
import com.maizeyield.exception.ResourceNotFoundException
import com.maizeyield.exception.UnauthorizedAccessException
import com.maizeyield.model.PlantingSession
import com.maizeyield.model.User
import com.maizeyield.repository.*
import com.maizeyield.service.FarmService
import com.maizeyield.service.PlantingSessionService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class PlantingSessionServiceImpl(
    private val plantingSessionRepository: PlantingSessionRepository,
    private val farmRepository: FarmRepository,
    private val userRepository: UserRepository,
    private val maizeVarietyRepository: MaizeVarietyRepository,
    private val yieldPredictionRepository: YieldPredictionRepository,
    private val recommendationRepository: RecommendationRepository,
    private val yieldHistoryRepository: YieldHistoryRepository,
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
        val recommendations = recommendationRepository.findByPlantingSessionOrderByRecommendationDateDesc(session)
            .take(5) // Latest 5 recommendations
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
                    confidence = recommendation.confidence
                )
            }

        return PlantingSessionDetailResponse(
            id = session.id!!,
            farmId = session.farm.id!!,
            maizeVariety = mapToMaizeVarietyResponse(session.maizeVariety),
            plantingDate = session.plantingDate,
            expectedHarvestDate = session.expectedHarvestDate,
            seedRateKgPerHectare = session.seedRateKgPerHectare,
            rowSpacingCm = session.rowSpacingCm,
            fertilizerType = session.fertilizerType,
            fertilizerAmountKgPerHectare = session.fertilizerAmountKgPerHectare,
            irrigationMethod = session.irrigationMethod,
            notes = session.notes,
            daysFromPlanting = ChronoUnit.DAYS.between(session.plantingDate, LocalDate.now()).toInt(),
            growthStage = calculateGrowthStage(session),
            predictions = latestPrediction?.let { listOf(it) } ?: emptyList(),
            recommendations = recommendations,
            pestsDiseases = emptyList(), // TODO: implement when PestsDiseaseResponse exists
            farmActivities = emptyList(), // TODO: implement when FarmActivityResponse exists
            weatherData = emptyList() // TODO: implement when WeatherDataResponse exists
        )
    }

    @Transactional
    override fun createPlantingSession(userId: Long, farmId: Long, request: PlantingSessionCreateRequest): PlantingSessionResponse {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to create planting sessions for this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        val maizeVariety = maizeVarietyRepository.findById(request.maizeVarietyId)
            .orElseThrow { ResourceNotFoundException("Maize variety not found with ID: ${request.maizeVarietyId}") }

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

        return plantingDate.plusDays(maizeVariety.maturityDays.toLong())
    }

    override fun hasAccessToPlantingSession(userId: Long, sessionId: Long): Boolean {
        val session = plantingSessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $sessionId") }

        return farmService.isFarmOwner(userId, session.farm.id!!)
    }

    // NEW METHODS - These are the ones that were TODO

    override fun getAllPlantingSessions(
        userId: Long,
        pageable: Pageable,
        search: String
    ): Page<PlantingSessionResponse> {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with ID: $userId") }

        // Get all farms owned by the user
        val userFarms = farmRepository.findByUser(user)

        if (userFarms.isEmpty()) {
            return PageImpl(emptyList(), pageable, 0)
        }

        val sessions = if (search.isBlank()) {
            // Get all sessions for user's farms with pagination
            plantingSessionRepository.findByFarmIn(userFarms, pageable)
        } else {
            // Search sessions by farm name or maize variety name
            plantingSessionRepository.findByFarmInAndFarmNameContainingIgnoreCaseOrMaizeVarietyNameContainingIgnoreCase(
                userFarms, search, search, pageable
            )
        }

        val sessionResponses = sessions.content.map { session ->
            mapToPlantingSessionResponse(session)
        }

        return PageImpl(sessionResponses, pageable, sessions.totalElements)
    }

    override fun isSessionOwner(userId: Long, sessionId: Long): Boolean {
        val session = plantingSessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $sessionId") }

        return farmService.isFarmOwner(userId, session.farm.id!!)
    }

    override fun getPlantingSessionStatus(
        userId: Long,
        sessionId: Long
    ): Map<String, Any> {
        if (!isSessionOwner(userId, sessionId)) {
            throw UnauthorizedAccessException("You don't have permission to access this planting session")
        }

        val session = plantingSessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $sessionId") }

        val daysFromPlanting = ChronoUnit.DAYS.between(session.plantingDate, LocalDate.now()).toInt()
        val status = calculateSessionStatus(session)
        val growthStage = calculateGrowthStage(session)

        // Get yield history if harvested
        val yieldHistory = yieldHistoryRepository.findByPlantingSession(session).firstOrNull()

        // Get latest prediction
        val latestPrediction = yieldPredictionRepository.findLatestByPlantingSession(session).orElse(null)

        // Get pending recommendations count
        val pendingRecommendations = recommendationRepository.countUnimplementedHighPriorityRecommendations(session)

        return mapOf(
            "sessionId" to sessionId,
            "farmName" to session.farm.name,
            "maizeVariety" to session.maizeVariety.name,
            "status" to status,
            "growthStage" to growthStage,
            "daysFromPlanting" to daysFromPlanting,
            "plantingDate" to session.plantingDate,
            "expectedHarvestDate" to (session.expectedHarvestDate ?: "Not set"),
            "actualHarvestDate" to (yieldHistory?.harvestDate ?: "Not harvested"),
            "actualYield" to (yieldHistory?.yieldTonsPerHectare?.toString() ?: "No data"),
            "predictedYield" to (latestPrediction?.predictedYieldTonsPerHectare?.toString() ?: "No prediction"),
            "pendingRecommendations" to pendingRecommendations,
            "lastUpdated" to LocalDateTime.now()
        )
    }

    @Transactional
    override fun updatePlantingSessionStatus(
        userId: Long,
        sessionId: Long,
        status: String
    ): Boolean {
        if (!isSessionOwner(userId, sessionId)) {
            throw UnauthorizedAccessException("You don't have permission to update this planting session")
        }

        val session = plantingSessionRepository.findById(sessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $sessionId") }

        // Validate status
        val validStatuses = listOf("PLANTED", "GROWING", "HARVESTED", "FAILED")
        if (!validStatuses.contains(status.uppercase())) {
            throw IllegalArgumentException("Invalid status: $status. Valid statuses are: ${validStatuses.joinToString(", ")}")
        }

        // For now, we store status information in notes or handle it through business logic
        // Since the entity doesn't have a status field, we use calculated status based on dates
        // This method could be used to trigger status-specific actions like creating yield history

        when (status.uppercase()) {
            "HARVESTED" -> {
                // Could trigger creation of yield history record
                // or update expected harvest date to actual harvest date
                if (session.expectedHarvestDate == null || session.expectedHarvestDate!!.isAfter(LocalDate.now())) {
                    // Update expected harvest date to today if not set or in future
                    val updatedSession = session.copy(expectedHarvestDate = LocalDate.now())
                    plantingSessionRepository.save(updatedSession)
                }
            }
            "FAILED" -> {
                // Could add a note about failure
                val failureNote = "${session.notes ?: ""}\n[${LocalDate.now()}] Session marked as failed."
                val updatedSession = session.copy(notes = failureNote.trim())
                plantingSessionRepository.save(updatedSession)
            }
        }

        return true
    }

    override fun getActivePlantingSessions(userId: Long): List<PlantingSessionResponse> {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with ID: $userId") }

        val userFarms = farmRepository.findByUser(user)

        return userFarms.flatMap { farm ->
            plantingSessionRepository.findActiveSessionsByFarm(farm, LocalDate.now())
        }.map { session ->
            mapToPlantingSessionResponse(session)
        }
    }

    override fun getPlantingSessionsByDateRange(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<PlantingSessionResponse> {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with ID: $userId") }

        val userFarms = farmRepository.findByUser(user)

        return userFarms.flatMap { farm ->
            plantingSessionRepository.findByFarmAndPlantingDateBetween(farm, startDate, endDate)
        }.map { session ->
            mapToPlantingSessionResponse(session)
        }
    }

    // Helper methods
    private fun mapToPlantingSessionResponse(session: PlantingSession): PlantingSessionResponse {
        val daysFromPlanting = ChronoUnit.DAYS.between(session.plantingDate, LocalDate.now()).toInt()
        val growthStage = calculateGrowthStage(session)

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
            maizeVariety = mapToMaizeVarietyResponse(session.maizeVariety),
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

    private fun mapToMaizeVarietyResponse(variety: com.maizeyield.model.MaizeVariety): MaizeVarietyResponse {
        return MaizeVarietyResponse(
            id = variety.id!!,
            name = variety.name,
            maturityDays = variety.maturityDays,
            optimalTemperatureMin = variety.optimalTemperatureMin,
            optimalTemperatureMax = variety.optimalTemperatureMax,
            droughtResistance = variety.droughtResistance,
            diseaseResistance = variety.diseaseResistance,
            description = variety.description
        )
    }

    private fun calculateSessionStatus(session: PlantingSession): String {
        val today = LocalDate.now()
        val plantingDate = session.plantingDate
        val expectedHarvestDate = session.expectedHarvestDate

        // Check if there's actual yield history (means harvested)
        val hasYieldHistory = yieldHistoryRepository.findByPlantingSession(session).isNotEmpty()
        if (hasYieldHistory) {
            return "HARVESTED"
        }

        // Check if session failed (indicated by notes containing failure keywords)
        val notes = session.notes?.lowercase() ?: ""
        if (notes.contains("failed") || notes.contains("failure") || notes.contains("abandoned")) {
            return "FAILED"
        }

        // Check if past expected harvest date without yield history
        if (expectedHarvestDate != null && today.isAfter(expectedHarvestDate.plusWeeks(2))) {
            return "FAILED" // Likely failed if 2 weeks past expected harvest with no yield recorded
        }

        // Check if growing (planted and before/at expected harvest)
        val daysFromPlanting = ChronoUnit.DAYS.between(plantingDate, today).toInt()
        if (daysFromPlanting > 7) { // More than a week from planting
            return "GROWING"
        }

        // Recently planted
        return "PLANTED"
    }

    private fun calculateGrowthStage(session: PlantingSession): String {
        val daysFromPlanting = ChronoUnit.DAYS.between(session.plantingDate, LocalDate.now()).toInt()
        val maturityDays = session.maizeVariety.maturityDays

        return when {
            daysFromPlanting <= 14 -> "GERMINATION"
            daysFromPlanting <= 30 -> "VEGETATIVE_EARLY"
            daysFromPlanting <= maturityDays * 0.4 -> "VEGETATIVE_LATE"
            daysFromPlanting <= maturityDays * 0.6 -> "TASSELING"
            daysFromPlanting <= maturityDays * 0.8 -> "GRAIN_FILLING"
            daysFromPlanting <= maturityDays -> "MATURITY"
            else -> "POST_HARVEST"
        }
    }
}