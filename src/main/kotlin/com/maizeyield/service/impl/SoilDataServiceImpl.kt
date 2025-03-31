package com.maizeyield.service.impl

import com.maizeyield.dto.SoilDataCreateRequest
import com.maizeyield.dto.SoilDataResponse
import com.maizeyield.dto.SoilDataUpdateRequest
import com.maizeyield.exception.ResourceNotFoundException
import com.maizeyield.exception.UnauthorizedAccessException
import com.maizeyield.model.SoilData
import com.maizeyield.repository.FarmRepository
import com.maizeyield.repository.SoilDataRepository
import com.maizeyield.service.FarmService
import com.maizeyield.service.SoilDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class SoilDataServiceImpl(
    private val soilDataRepository: SoilDataRepository,
    private val farmRepository: FarmRepository,
    private val farmService: FarmService
) : SoilDataService {

    override fun getSoilDataByFarmId(userId: Long, farmId: Long): List<SoilDataResponse> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return soilDataRepository.findByFarm(farm).map { soilData ->
            mapToSoilDataResponse(soilData)
        }
    }

    override fun getSoilDataById(userId: Long, soilDataId: Long): SoilDataResponse {
        val soilData = soilDataRepository.findById(soilDataId)
            .orElseThrow { ResourceNotFoundException("Soil data not found with ID: $soilDataId") }

        if (!farmService.isFarmOwner(userId, soilData.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access this soil data")
        }

        return mapToSoilDataResponse(soilData)
    }

    @Transactional
    override fun addSoilData(userId: Long, farmId: Long, request: SoilDataCreateRequest): SoilDataResponse {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to add soil data to this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        val soilData = SoilData(
            farm = farm,
            soilType = request.soilType,
            phLevel = request.phLevel,
            organicMatterPercentage = request.organicMatterPercentage,
            nitrogenContent = request.nitrogenContent,
            phosphorusContent = request.phosphorusContent,
            potassiumContent = request.potassiumContent,
            moistureContent = request.moistureContent,
            sampleDate = request.sampleDate
        )

        val savedSoilData = soilDataRepository.save(soilData)

        return mapToSoilDataResponse(savedSoilData)
    }

    @Transactional
    override fun updateSoilData(userId: Long, soilDataId: Long, request: SoilDataUpdateRequest): SoilDataResponse {
        val soilData = soilDataRepository.findById(soilDataId)
            .orElseThrow { ResourceNotFoundException("Soil data not found with ID: $soilDataId") }

        if (!farmService.isFarmOwner(userId, soilData.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to update this soil data")
        }

        val updatedSoilData = soilData.copy(
            soilType = request.soilType ?: soilData.soilType,
            phLevel = request.phLevel ?: soilData.phLevel,
            organicMatterPercentage = request.organicMatterPercentage ?: soilData.organicMatterPercentage,
            nitrogenContent = request.nitrogenContent ?: soilData.nitrogenContent,
            phosphorusContent = request.phosphorusContent ?: soilData.phosphorusContent,
            potassiumContent = request.potassiumContent ?: soilData.potassiumContent,
            moistureContent = request.moistureContent ?: soilData.moistureContent,
            sampleDate = request.sampleDate ?: soilData.sampleDate
        )

        val savedSoilData = soilDataRepository.save(updatedSoilData)

        return mapToSoilDataResponse(savedSoilData)
    }

    @Transactional
    override fun deleteSoilData(userId: Long, soilDataId: Long): Boolean {
        val soilData = soilDataRepository.findById(soilDataId)
            .orElseThrow { ResourceNotFoundException("Soil data not found with ID: $soilDataId") }

        if (!farmService.isFarmOwner(userId, soilData.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to delete this soil data")
        }

        soilDataRepository.delete(soilData)
        return true
    }

    override fun getLatestSoilData(userId: Long, farmId: Long): SoilDataResponse? {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return soilDataRepository.findLatestByFarm(farm)
            .map { mapToSoilDataResponse(it) }
            .orElse(null)
    }

    override fun getSoilDataByDateRange(
        userId: Long,
        farmId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<SoilDataResponse> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return soilDataRepository.findByFarm(farm)
            .filter { it.sampleDate in startDate..endDate }
            .map { mapToSoilDataResponse(it) }
    }

    // Helper method to map SoilData entity to SoilDataResponse DTO
    private fun mapToSoilDataResponse(soilData: SoilData): SoilDataResponse {
        // Calculate soil health score (simple example, could be more complex in reality)
        val soilHealthScore = calculateSoilHealthScore(soilData)

        return SoilDataResponse(
            id = soilData.id!!,
            farmId = soilData.farm.id!!,
            soilType = soilData.soilType,
            phLevel = soilData.phLevel,
            organicMatterPercentage = soilData.organicMatterPercentage,
            nitrogenContent = soilData.nitrogenContent,
            phosphorusContent = soilData.phosphorusContent,
            potassiumContent = soilData.potassiumContent,
            moistureContent = soilData.moistureContent,
            sampleDate = soilData.sampleDate,
            soilHealthScore = soilHealthScore,
            fertilizerRecommendation = generateFertilizerRecommendation(soilData)
        )
    }

    // Simple soil health score calculation
    private fun calculateSoilHealthScore(soilData: SoilData): Int? {
        // If we don't have enough data, return null
        if (soilData.phLevel == null || soilData.organicMatterPercentage == null) {
            return null
        }

        var score = 0

        // pH level scoring (6.0-7.0 is optimal for maize)
        soilData.phLevel?.let {
            score += when (it.toDouble()) {
                in 6.0..7.0 -> 30 // Optimal
                in 5.5..5.9, in 7.1..7.5 -> 20 // Good
                in 5.0..5.4, in 7.6..8.0 -> 10 // Fair
                else -> 0 // Poor
            }
        }

        // Organic matter scoring (3-5% is good for crops)
        soilData.organicMatterPercentage?.let {
            score += when (it.toDouble()) {
                in 3.0..5.0 -> 30 // Optimal
                in 2.0..2.9, in 5.1..6.0 -> 20 // Good
                in 1.0..1.9, in 6.1..7.0 -> 10 // Fair
                else -> 0 // Poor
            }
        }

        // Nitrogen content scoring
        soilData.nitrogenContent?.let {
            score += when (it.toDouble()) {
                in 1.8..2.5 -> 20 // Optimal
                in 1.0..1.7 -> 10 // Good
                in 0.5..0.9 -> 5 // Fair
                else -> 0 // Poor
            }
        }

        // Phosphorus content scoring
        soilData.phosphorusContent?.let {
            score += when (it.toDouble()) {
                in 1.5..2.5 -> 10 // Optimal
                in 1.0..1.4 -> 5 // Good
                else -> 0 // Fair/Poor
            }
        }

        // Potassium content scoring
        soilData.potassiumContent?.let {
            score += when (it.toDouble()) {
                in 2.0..3.0 -> 10 // Optimal
                in 1.5..1.9 -> 5 // Good
                else -> 0 // Fair/Poor
            }
        }

        return score
    }

    // Generate simple fertilizer recommendation based on soil data
    private fun generateFertilizerRecommendation(soilData: SoilData): String? {
        // If we don't have enough data, return null
        if (soilData.nitrogenContent == null && soilData.phosphorusContent == null && soilData.potassiumContent == null) {
            return null
        }

        val recommendations = mutableListOf<String>()

        // pH level recommendations
        soilData.phLevel?.let {
            when (it.toDouble()) {
                in 0.0..5.5 -> recommendations.add("Apply lime to increase soil pH")
                in 7.5..14.0 -> recommendations.add("Apply organic matter to decrease soil pH")
                else -> {}
            }
        }

        // Nitrogen recommendations
        soilData.nitrogenContent?.let {
            if (it.toDouble() < 1.5) {
                recommendations.add("Apply nitrogen-rich fertilizer such as urea or ammonium nitrate")
            }
        }

        // Phosphorus recommendations
        soilData.phosphorusContent?.let {
            if (it.toDouble() < 1.2) {
                recommendations.add("Apply phosphorus-rich fertilizer such as superphosphate")
            }
        }

        // Potassium recommendations
        soilData.potassiumContent?.let {
            if (it.toDouble() < 1.5) {
                recommendations.add("Apply potassium-rich fertilizer such as potassium chloride")
            }
        }

        // Organic matter recommendations
        soilData.organicMatterPercentage?.let {
            if (it.toDouble() < 2.0) {
                recommendations.add("Increase organic matter by adding compost or manure")
            }
        }

        return if (recommendations.isEmpty()) {
            "Soil conditions are good for maize cultivation. No specific fertilizer recommendations."
        } else {
            recommendations.joinToString(". ") + "."
        }
    }
}