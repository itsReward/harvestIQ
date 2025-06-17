package com.maizeyield.service.impl

import com.maizeyield.dto.*
import com.maizeyield.exception.ResourceNotFoundException
import com.maizeyield.exception.UnauthorizedAccessException
import com.maizeyield.model.Farm
import com.maizeyield.repository.FarmRepository
import com.maizeyield.repository.PlantingSessionRepository
import com.maizeyield.repository.SoilDataRepository
import com.maizeyield.repository.UserRepository
import com.maizeyield.service.FarmService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class FarmServiceImpl(
    private val farmRepository: FarmRepository,
    private val userRepository: UserRepository,
    private val soilDataRepository: SoilDataRepository,
    private val plantingSessionRepository: PlantingSessionRepository
) : FarmService {

    override fun getFarmsByUserId(userId: Long): List<FarmResponse> {
        val user = userRepository.findById(userId)
            .orElseThrow { ResourceNotFoundException("User not found with ID: $userId") }

        return farmRepository.findByUser(user).map { farm ->
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
        TODO("Not yet implemented")
    }

    override fun getFarmStatistics(
        userId: Long,
        farmId: Long
    ): Map<String, Any> {
        TODO("Not yet implemented")
    }

    override fun transferFarmOwnership(
        currentUserId: Long,
        farmId: Long,
        newOwnerId: Long
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun getFarmsByRegion(region: String): List<FarmResponse> {
        TODO("Not yet implemented")
    }

    override fun getFarmStatisticsSummary(): Map<String, Any> {
        TODO("Not yet implemented")
    }

}