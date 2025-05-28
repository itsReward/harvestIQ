package com.maizeyield.service.impl

import com.maizeyield.dto.YieldHistoryCreateRequest
import com.maizeyield.dto.YieldHistoryResponse
import com.maizeyield.dto.YieldHistoryUpdateRequest
import com.maizeyield.exception.ResourceNotFoundException
import com.maizeyield.exception.UnauthorizedAccessException
import com.maizeyield.model.YieldHistory
import com.maizeyield.repository.FarmRepository
import com.maizeyield.repository.PlantingSessionRepository
import com.maizeyield.repository.YieldHistoryRepository
import com.maizeyield.service.FarmService
import com.maizeyield.service.YieldHistoryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class YieldHistoryServiceImpl(
    private val yieldHistoryRepository: YieldHistoryRepository,
    private val plantingSessionRepository: PlantingSessionRepository,
    private val farmRepository: FarmRepository,
    private val farmService: FarmService
) : YieldHistoryService {

    override fun getYieldHistoryByPlantingSessionId(userId: Long, plantingSessionId: Long): List<YieldHistoryResponse> {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access yield history for this planting session")
        }

        return yieldHistoryRepository.findByPlantingSession(session).map { yieldHistory ->
            mapToYieldHistoryResponse(yieldHistory)
        }
    }

    override fun getYieldHistoryById(userId: Long, yieldHistoryId: Long): YieldHistoryResponse {
        val yieldHistory = yieldHistoryRepository.findById(yieldHistoryId)
            .orElseThrow { ResourceNotFoundException("Yield history not found with ID: $yieldHistoryId") }

        if (!farmService.isFarmOwner(userId, yieldHistory.plantingSession.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access this yield history")
        }

        return mapToYieldHistoryResponse(yieldHistory)
    }

    @Transactional
    override fun addYieldHistory(userId: Long, plantingSessionId: Long, request: YieldHistoryCreateRequest): YieldHistoryResponse {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to add yield history to this planting session")
        }

        // Validate harvest date is not before planting date
        if (request.harvestDate.isBefore(session.plantingDate)) {
            throw IllegalArgumentException("Harvest date cannot be before planting date")
        }

        val yieldHistory = YieldHistory(
            plantingSession = session,
            harvestDate = request.harvestDate,
            yieldTonsPerHectare = request.yieldTonsPerHectare,
            qualityRating = request.qualityRating,
            notes = request.notes
        )

        val savedYieldHistory = yieldHistoryRepository.save(yieldHistory)
        return mapToYieldHistoryResponse(savedYieldHistory)
    }

    @Transactional
    override fun updateYieldHistory(userId: Long, yieldHistoryId: Long, request: YieldHistoryUpdateRequest): YieldHistoryResponse {
        val yieldHistory = yieldHistoryRepository.findById(yieldHistoryId)
            .orElseThrow { ResourceNotFoundException("Yield history not found with ID: $yieldHistoryId") }

        if (!farmService.isFarmOwner(userId, yieldHistory.plantingSession.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to update this yield history")
        }

        // Validate harvest date is not before planting date if being updated
        if (request.harvestDate != null && request.harvestDate.isBefore(yieldHistory.plantingSession.plantingDate)) {
            throw IllegalArgumentException("Harvest date cannot be before planting date")
        }

        val updatedYieldHistory = yieldHistory.copy(
            harvestDate = request.harvestDate ?: yieldHistory.harvestDate,
            yieldTonsPerHectare = request.yieldTonsPerHectare ?: yieldHistory.yieldTonsPerHectare,
            qualityRating = request.qualityRating ?: yieldHistory.qualityRating,
            notes = request.notes ?: yieldHistory.notes
        )

        val savedYieldHistory = yieldHistoryRepository.save(updatedYieldHistory)
        return mapToYieldHistoryResponse(savedYieldHistory)
    }

    @Transactional
    override fun deleteYieldHistory(userId: Long, yieldHistoryId: Long): Boolean {
        val yieldHistory = yieldHistoryRepository.findById(yieldHistoryId)
            .orElseThrow { ResourceNotFoundException("Yield history not found with ID: $yieldHistoryId") }

        if (!farmService.isFarmOwner(userId, yieldHistory.plantingSession.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to delete this yield history")
        }

        yieldHistoryRepository.delete(yieldHistory)
        return true
    }

    override fun getYieldHistoryByFarmId(userId: Long, farmId: Long): List<YieldHistoryResponse> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access yield history for this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return yieldHistoryRepository.findByFarm(farm).map { yieldHistory ->
            mapToYieldHistoryResponse(yieldHistory)
        }
    }

    override fun getYieldHistoryByDateRange(userId: Long, farmId: Long, startDate: LocalDate, endDate: LocalDate): List<YieldHistoryResponse> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access yield history for this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return yieldHistoryRepository.findByFarmAndHarvestDateBetween(farm, startDate, endDate).map { yieldHistory ->
            mapToYieldHistoryResponse(yieldHistory)
        }
    }

    override fun calculateAverageYield(userId: Long, farmId: Long): BigDecimal? {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access yield history for this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return yieldHistoryRepository.findAverageYieldByFarm(farm)
    }

    override fun getHighestYield(userId: Long, farmId: Long): YieldHistoryResponse? {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access yield history for this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return yieldHistoryRepository.findHighestYieldByFarm(farm)
            .map { mapToYieldHistoryResponse(it) }
            .orElse(null)
    }

    override fun getLatestYieldHistory(userId: Long, plantingSessionId: Long): YieldHistoryResponse? {
        val session = plantingSessionRepository.findById(plantingSessionId)
            .orElseThrow { ResourceNotFoundException("Planting session not found with ID: $plantingSessionId") }

        if (!farmService.isFarmOwner(userId, session.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access yield history for this planting session")
        }

        return yieldHistoryRepository.findLatestByPlantingSession(session)
            .map { mapToYieldHistoryResponse(it) }
            .orElse(null)
    }

    // Helper method to map YieldHistory entity to YieldHistoryResponse DTO
    private fun mapToYieldHistoryResponse(yieldHistory: YieldHistory): YieldHistoryResponse {
        val session = yieldHistory.plantingSession
        val daysToMaturity = yieldHistory.harvestDate.toEpochDay() - session.plantingDate.toEpochDay()

        return YieldHistoryResponse(
            id = yieldHistory.id!!,
            plantingSessionId = session.id!!,
            harvestDate = yieldHistory.harvestDate,
            yieldTonsPerHectare = yieldHistory.yieldTonsPerHectare,
            qualityRating = yieldHistory.qualityRating,
            notes = yieldHistory.notes,
            farmName = session.farm.name,
            maizeVarietyName = session.maizeVariety.name,
            plantingDate = session.plantingDate,
            daysToMaturity = daysToMaturity.toInt()
        )
    }
}