package com.maizeyield.service.impl

import com.maizeyield.dto.MaizeVarietyCreateRequest
import com.maizeyield.dto.MaizeVarietyResponse
import com.maizeyield.dto.MaizeVarietyUpdateRequest
import com.maizeyield.exception.ResourceNotFoundException
import com.maizeyield.model.MaizeVariety
import com.maizeyield.repository.MaizeVarietyRepository
import com.maizeyield.service.MaizeVarietyService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MaizeVarietyServiceImpl(
    private val maizeVarietyRepository: MaizeVarietyRepository
) : MaizeVarietyService {

    override fun getAllMaizeVarieties(): List<MaizeVarietyResponse> {
        return maizeVarietyRepository.findAll().map { variety ->
            mapToMaizeVarietyResponse(variety)
        }
    }

    override fun getMaizeVarietyById(varietyId: Long): MaizeVarietyResponse {
        val variety = maizeVarietyRepository.findById(varietyId)
            .orElseThrow { ResourceNotFoundException("Maize variety not found with ID: $varietyId") }

        return mapToMaizeVarietyResponse(variety)
    }

    @Transactional
    override fun createMaizeVariety(request: MaizeVarietyCreateRequest): MaizeVarietyResponse {
        // Check if variety with same name already exists
        if (maizeVarietyRepository.findByName(request.name).isPresent) {
            throw IllegalArgumentException("Maize variety with name '${request.name}' already exists")
        }

        val variety = MaizeVariety(
            name = request.name,
            maturityDays = request.maturityDays,
            optimalTemperatureMin = request.optimalTemperatureMin,
            optimalTemperatureMax = request.optimalTemperatureMax,
            droughtResistance = request.droughtResistance,
            diseaseResistance = request.diseaseResistance,
            description = request.description
        )

        val savedVariety = maizeVarietyRepository.save(variety)
        return mapToMaizeVarietyResponse(savedVariety)
    }

    @Transactional
    override fun updateMaizeVariety(varietyId: Long, request: MaizeVarietyUpdateRequest): MaizeVarietyResponse {
        val variety = maizeVarietyRepository.findById(varietyId)
            .orElseThrow { ResourceNotFoundException("Maize variety not found with ID: $varietyId") }

        // Check if another variety with the same name exists (if name is being updated)
        if (request.name != null && request.name != variety.name) {
            maizeVarietyRepository.findByName(request.name).ifPresent {
                if (it.id != varietyId) {
                    throw IllegalArgumentException("Maize variety with name '${request.name}' already exists")
                }
            }
        }

        val updatedVariety = variety.copy(
            name = request.name ?: variety.name,
            maturityDays = request.maturityDays ?: variety.maturityDays,
            optimalTemperatureMin = request.optimalTemperatureMin ?: variety.optimalTemperatureMin,
            optimalTemperatureMax = request.optimalTemperatureMax ?: variety.optimalTemperatureMax,
            droughtResistance = request.droughtResistance ?: variety.droughtResistance,
            diseaseResistance = request.diseaseResistance ?: variety.diseaseResistance,
            description = request.description ?: variety.description
        )

        val savedVariety = maizeVarietyRepository.save(updatedVariety)
        return mapToMaizeVarietyResponse(savedVariety)
    }

    @Transactional
    override fun deleteMaizeVariety(varietyId: Long): Boolean {
        if (!maizeVarietyRepository.existsById(varietyId)) {
            throw ResourceNotFoundException("Maize variety not found with ID: $varietyId")
        }

        maizeVarietyRepository.deleteById(varietyId)
        return true
    }

    override fun getDroughtResistantVarieties(): List<MaizeVarietyResponse> {
        return maizeVarietyRepository.findByDroughtResistance(true).map { variety ->
            mapToMaizeVarietyResponse(variety)
        }
    }

    override fun getVarietiesByMaturityRange(minDays: Int, maxDays: Int): List<MaizeVarietyResponse> {
        return maizeVarietyRepository.findByMaturityDaysBetween(minDays, maxDays).map { variety ->
            mapToMaizeVarietyResponse(variety)
        }
    }

    // Helper method to map MaizeVariety entity to MaizeVarietyResponse DTO
    private fun mapToMaizeVarietyResponse(variety: MaizeVariety): MaizeVarietyResponse {
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
}