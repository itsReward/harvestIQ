package com.maizeyield.service.impl

import com.maizeyield.dto.*
import com.maizeyield.exception.ResourceNotFoundException
import com.maizeyield.exception.UnauthorizedAccessException
import com.maizeyield.model.Farm
import com.maizeyield.repository.*
import com.maizeyield.service.FarmService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class FarmServiceImpl(
    private val farmRepository: FarmRepository,
    private val userRepository: UserRepository,
    private val soilDataRepository: SoilDataRepository,
    private val plantingSessionRepository: PlantingSessionRepository,
    private val yieldPredictionRepository: YieldPredictionRepository,
    private val weatherDataRepository: WeatherDataRepository
) : FarmService {


    @Transactional(readOnly = true)
    override fun getAllFarmsForAdmin(pageable: Pageable, search: String): Page<FarmResponse> {
        val farms = if (search.isBlank()) {
            // Get ALL farms in the system with pagination
            farmRepository.findAll(pageable)
        } else {
            // Search ALL farms by name or location
            farmRepository.findByNameContainingIgnoreCaseOrLocationContainingIgnoreCase(
                search, search, pageable
            )
        }

        val farmResponses = farms.content.map { farm ->
            mapToFarmResponse(farm)
        }

        return PageImpl(farmResponses, pageable, farms.totalElements)
    }

    override fun getFarmsByUserId(userId: Long): List<FarmResponse> {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with ID: $userId") }

        return farmRepository.findByUser(user).map { farm ->
            mapToFarmResponse(farm)
        }
    }

    override fun getFarmById(userId: Long, farmId: Long): FarmDetailResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with ID: $userId") }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        if (farm.user.id != userId) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        // Get latest soil data if available
        val latestSoilData = soilDataRepository.findLatestByFarm(farm).orElse(null)?.let { soilData ->
            SoilDataResponse(
                id = soilData.id!!,
                farmId = farm.id!!,
                soilType = soilData.soilType,
                phLevel = soilData.phLevel,
                organicMatterPercentage = soilData.organicMatterPercentage,
                nitrogenContent = soilData.nitrogenContent,
                phosphorusContent = soilData.phosphorusContent,
                potassiumContent = soilData.potassiumContent,
                moistureContent = soilData.moistureContent,
                sampleDate = soilData.sampleDate
            )
        }

        // Get active planting sessions
        val activePlantingSessions = plantingSessionRepository.findActiveSessionsByFarm(farm, LocalDate.now())
            .map { session ->
                PlantingSessionResponse(
                    id = session.id!!,
                    farmId = farm.id!!,
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
                    daysFromPlanting = LocalDate.now().toEpochDay().minus(session.plantingDate.toEpochDay()).toInt()
                )
            }

        return FarmDetailResponse(
            id = farm.id!!,
            name = farm.name,
            location = farm.location,
            sizeHectares = farm.sizeHectares,
            latitude = farm.latitude,
            longitude = farm.longitude,
            elevation = farm.elevation,
            createdAt = farm.createdAt,
            soilData = latestSoilData,
            activePlantingSessions = activePlantingSessions
        )
    }

    @Transactional
    override fun createFarm(userId: Long, request: FarmCreateRequest): FarmResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with ID: $userId") }

        val farm = Farm(
            user = user,
            name = request.name,
            location = request.location,
            sizeHectares = request.sizeHectares,
            latitude = request.latitude,
            longitude = request.longitude,
            elevation = request.elevation
        )

        val savedFarm = farmRepository.save(farm)

        return FarmResponse(
            id = savedFarm.id!!,
            name = savedFarm.name,
            location = savedFarm.location,
            sizeHectares = savedFarm.sizeHectares,
            latitude = savedFarm.latitude,
            longitude = savedFarm.longitude,
            elevation = savedFarm.elevation,
            createdAt = savedFarm.createdAt
        )
    }

    @Transactional
    override fun updateFarm(userId: Long, farmId: Long, request: FarmUpdateRequest): FarmResponse {
        if (!isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to update this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        // Update only non-null fields from the request
        val updatedFarm = farm.copy(
            name = request.name ?: farm.name,
            location = request.location ?: farm.location,
            sizeHectares = request.sizeHectares ?: farm.sizeHectares,
            latitude = request.latitude ?: farm.latitude,
            longitude = request.longitude ?: farm.longitude,
            elevation = request.elevation ?: farm.elevation
        )

        val savedFarm = farmRepository.save(updatedFarm)

        return FarmResponse(
            id = savedFarm.id!!,
            name = savedFarm.name,
            location = savedFarm.location,
            sizeHectares = savedFarm.sizeHectares,
            latitude = savedFarm.latitude,
            longitude = savedFarm.longitude,
            elevation = savedFarm.elevation,
            createdAt = savedFarm.createdAt
        )
    }

    @Transactional
    override fun deleteFarm(userId: Long, farmId: Long): Boolean {
        if (!isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to delete this farm")
        }

        if (!farmRepository.existsById(farmId)) {
            throw ResourceNotFoundException("Farm not found with ID: $farmId")
        }

        farmRepository.deleteById(farmId)
        return true
    }

    override fun isFarmOwner(userId: Long, farmId: Long): Boolean {
        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return farm.user.id == userId
    }

    override fun getTotalFarmCount(): Long {
        return farmRepository.count()
    }

    override fun getAllFarms(
        userId: Long,
        pageable: Pageable,
        search: String
    ): Page<FarmResponse> {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with ID: $userId") }

        val farms = if (search.isBlank()) {
            // Get all farms for the user with pagination
            farmRepository.findByUser(user, pageable)
        } else {
            // Search farms by name or location
            farmRepository.findByUserAndNameContainingIgnoreCaseOrUserAndLocationContainingIgnoreCase(
                user, search, user, search, pageable
            )
        }

        val farmResponses = farms.content.map { farm ->
            FarmResponse(
                id = farm.id!!,
                name = farm.name,
                location = farm.location,
                sizeHectares = farm.sizeHectares,
                latitude = farm.latitude,
                longitude = farm.longitude,
                elevation = farm.elevation,
                createdAt = farm.createdAt,
                ownerId = farm.user.id!!,
            )
        }

        return PageImpl(farmResponses, pageable, farms.totalElements)
    }

    override fun getFarmStatistics(
        userId: Long,
        farmId: Long
    ): Map<String, Any> {
        if (!isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm's statistics")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        // Get planting sessions count
        val totalPlantingSessions = plantingSessionRepository.findByFarm(farm).size
        val activePlantingSessions = plantingSessionRepository.findActiveSessionsByFarm(farm, LocalDate.now()).size

        // Get yield predictions statistics
        val yieldPredictions = yieldPredictionRepository.findByPlantingSessionFarmIdIn(listOf(farmId))
        val averageYield = if (yieldPredictions.isNotEmpty()) {
            yieldPredictions.map { it.predictedYieldTonsPerHectare.toDouble() }.average()
        } else 0.0

        val latestYield = yieldPredictions.maxByOrNull { it.predictionDate }?.predictedYieldTonsPerHectare?.toDouble()

        // Get soil data statistics
        val soilDataCount = soilDataRepository.findByFarm(farm).size
        val latestSoilData = soilDataRepository.findLatestByFarm(farm).orElse(null)

        // Calculate productivity metrics
        val productivityScore = calculateProductivityScore(farm, yieldPredictions)
        val farmAge = java.time.Period.between(farm.createdAt.toLocalDate(), LocalDate.now()).days

        return mapOf(
            "farmId" to farmId,
            "farmName" to farm.name,
            "farmSize" to farm.sizeHectares,
            "location" to farm.location,
            "farmAgeInDays" to farmAge,
            "totalPlantingSessions" to totalPlantingSessions,
            "activePlantingSessions" to activePlantingSessions,
            "averageYieldTonsPerHectare" to BigDecimal(averageYield).setScale(2, RoundingMode.HALF_UP),
            "latestYieldTonsPerHectare" to (latestYield?.let { BigDecimal(it).setScale(2, RoundingMode.HALF_UP) } ?: "N/A"),
            "totalYieldPredictions" to yieldPredictions.size,
            "soilDataRecords" to soilDataCount,
            "latestSoilPh" to (latestSoilData?.phLevel ?: "N/A"),
            "latestSoilType" to (latestSoilData?.soilType ?: "N/A"),
            "productivityScore" to BigDecimal(productivityScore).setScale(1, RoundingMode.HALF_UP),
            "lastUpdated" to LocalDateTime.now()
        )
    }

    @Transactional
    override fun transferFarmOwnership(
        currentUserId: Long,
        farmId: Long,
        newOwnerId: Long
    ): Boolean {
        // Verify current user owns the farm
        if (!isFarmOwner(currentUserId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to transfer this farm")
        }

        // Verify new owner exists
        val newOwner = userRepository.findById(newOwnerId)
            .orElseThrow { ResourceNotFoundException("New owner not found with ID: $newOwnerId") }

        // Prevent transferring to the same user
        if (currentUserId == newOwnerId) {
            throw IllegalArgumentException("Cannot transfer farm to the same owner")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        // Update farm ownership
        val updatedFarm = farm.copy(user = newOwner)
        farmRepository.save(updatedFarm)

        return true
    }

    override fun getFarmsByRegion(region: String): List<FarmResponse> {
        val farms = farmRepository.findByLocationContainingIgnoreCase(region)

        return farms.map { farm ->
            FarmResponse(
                id = farm.id!!,
                name = farm.name,
                location = farm.location,
                sizeHectares = farm.sizeHectares,
                latitude = farm.latitude,
                longitude = farm.longitude,
                elevation = farm.elevation,
                createdAt = farm.createdAt
            )
        }
    }

    override fun getFarmStatisticsSummary(): Map<String, Any> {
        val totalFarms = farmRepository.count()
        val allFarms = farmRepository.findAll()
        val totalSize = allFarms.sumOf { it.sizeHectares.toDouble() }

        // Get time-based statistics
        val now = LocalDateTime.now()
        val thisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
        val lastMonth = thisMonth.minusMonths(1)

        val farmsThisMonth = farmRepository.findByCreatedAtBetween(thisMonth, now).size
        val farmsLastMonth = farmRepository.findByCreatedAtBetween(lastMonth, thisMonth).size

        val farmGrowthPercentage = if (farmsLastMonth > 0) {
            ((farmsThisMonth - farmsLastMonth).toDouble() / farmsLastMonth.toDouble()) * 100.0
        } else {
            if (farmsThisMonth > 0) 100.0 else 0.0
        }

        // Get planting session statistics
        val totalPlantingSessions = plantingSessionRepository.count()
        val activePlantingSessions = plantingSessionRepository.countActiveSessionsSince(LocalDate.now().minusMonths(6))

        // Get average farm size
        val averageFarmSize = if (totalFarms > 0) {
            BigDecimal(totalSize / totalFarms.toDouble()).setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        // Get regional distribution
        val regionDistribution = allFarms
            .groupBy { extractRegion(it.location) }
            .mapValues { it.value.size }

        // Get largest and smallest farms
        val largestFarm = allFarms.maxByOrNull { it.sizeHectares }
        val smallestFarm = allFarms.minByOrNull { it.sizeHectares }

        // Get recent yield predictions average
        val recentPredictions = yieldPredictionRepository.findTopByOrderByCreatedAtDesc(100)
        val averageRecentYield = if (recentPredictions.isNotEmpty()) {
            recentPredictions.map { it.predictedYieldTonsPerHectare.toDouble() }.average()
        } else 0.0

        return mapOf(
            "totalFarms" to totalFarms,
            "totalAreaHectares" to BigDecimal(totalSize).setScale(2, RoundingMode.HALF_UP),
            "averageFarmSizeHectares" to averageFarmSize,
            "farmGrowthPercentage" to BigDecimal(farmGrowthPercentage).setScale(1, RoundingMode.HALF_UP),
            "farmsCreatedThisMonth" to farmsThisMonth,
            "farmsCreatedLastMonth" to farmsLastMonth,
            "totalPlantingSessions" to totalPlantingSessions,
            "activePlantingSessions" to activePlantingSessions,
            "regionDistribution" to regionDistribution,
            "largestFarmSize" to (largestFarm?.sizeHectares ?: "N/A"),
            "smallestFarmSize" to (smallestFarm?.sizeHectares ?: "N/A"),
            "averageRecentYieldTonsPerHectare" to BigDecimal(averageRecentYield).setScale(2, RoundingMode.HALF_UP),
            "generatedAt" to LocalDateTime.now()
        )
    }

    private fun calculateProductivityScore(farm: Farm, yieldPredictions: List<com.maizeyield.model.YieldPrediction>): Double {
        if (yieldPredictions.isEmpty()) return 0.0

        val averageYield = yieldPredictions.map { it.predictedYieldTonsPerHectare.toDouble() }.average()
        val farmSize = farm.sizeHectares.toDouble()
        val sessionCount = yieldPredictions.size

        // Base score on yield per hectare, with bonuses for consistency and farm activity
        val baseScore = (averageYield / farmSize) * 10
        val consistencyBonus = if (sessionCount > 5) 10.0 else sessionCount * 2.0
        val sizeEfficiencyBonus = if (farmSize > 0) kotlin.math.min(farmSize / 10, 5.0) else 0.0

        return kotlin.math.min(baseScore + consistencyBonus + sizeEfficiencyBonus, 100.0)
    }

    private fun extractRegion(location: String): String {
        // Simple region extraction - you can make this more sophisticated
        return location.split(",").firstOrNull()?.trim() ?: "Unknown"
    }

    private fun mapToFarmResponse(farm: Farm): FarmResponse {
        return FarmResponse(
            id = farm.id!!,
            name = farm.name,
            location = farm.location,
            sizeHectares = farm.sizeHectares,
            latitude = farm.latitude,
            longitude = farm.longitude,
            elevation = farm.elevation,
            createdAt = farm.createdAt,
            // POPULATE OWNER AND CONTACT INFORMATION
            ownerId = farm.user.id,
            ownerName = buildString {
                if (!farm.user.firstName.isNullOrBlank()) {
                    append(farm.user.firstName)
                }
                if (!farm.user.lastName.isNullOrBlank()) {
                    if (isNotEmpty()) append(" ")
                    append(farm.user.lastName)
                }
                if (isEmpty()) {
                    append(farm.user.username)
                }
            },
            ownerEmail = farm.user.email,
            ownerPhone = farm.user.phoneNumber
        )
    }
}