package com.maizeyield.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.LocalDate

// Request DTOs
data class PlantingSessionCreateRequest(
    val maizeVarietyId: Long,
    val plantingDate: LocalDate,
    val expectedHarvestDate: LocalDate? = null,
    val seedRateKgPerHectare: BigDecimal? = null,
    val rowSpacingCm: Int? = null,
    val fertilizerType: String? = null,
    val fertilizerAmountKgPerHectare: BigDecimal? = null,
    val irrigationMethod: String? = null,
    val notes: String? = null
)

data class PlantingSessionUpdateRequest(
    val expectedHarvestDate: LocalDate? = null,
    val seedRateKgPerHectare: BigDecimal? = null,
    val rowSpacingCm: Int? = null,
    val fertilizerType: String? = null,
    val fertilizerAmountKgPerHectare: BigDecimal? = null,
    val irrigationMethod: String? = null,
    val notes: String? = null
)

// Response DTOs
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PlantingSessionResponse(
    val id: Long,
    val farmId: Long,
    val maizeVariety: MaizeVarietyResponse,
    val plantingDate: LocalDate,
    val expectedHarvestDate: LocalDate? = null,
    val seedRateKgPerHectare: BigDecimal? = null,
    val rowSpacingCm: Int? = null,
    val fertilizerType: String? = null,
    val fertilizerAmountKgPerHectare: BigDecimal? = null,
    val areaPlanted: BigDecimal? = null,
    val irrigationMethod: String? = null,
    val notes: String? = null,
    val daysFromPlanting: Int? = null,
    val growthStage: String? = null,
    val latestPrediction: YieldPredictionResponse? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PlantingSessionDetailResponse(
    val id: Long,
    val farmId: Long,
    val maizeVariety: MaizeVarietyResponse,
    val plantingDate: LocalDate,
    val expectedHarvestDate: LocalDate? = null,
    val seedRateKgPerHectare: BigDecimal? = null,
    val rowSpacingCm: Int? = null,
    val fertilizerType: String? = null,
    val fertilizerAmountKgPerHectare: BigDecimal? = null,
    val irrigationMethod: String? = null,
    val notes: String? = null,
    val daysFromPlanting: Int? = null,
    val growthStage: String? = null,
    val predictions: List<YieldPredictionResponse> = emptyList(),
    val recommendations: List<RecommendationResponse> = emptyList(),
    val pestsDiseases: List<PestsDiseaseResponse> = emptyList(),
    val farmActivities: List<FarmActivityResponse> = emptyList(),
    val weatherData: List<WeatherDataResponse> = emptyList()
)