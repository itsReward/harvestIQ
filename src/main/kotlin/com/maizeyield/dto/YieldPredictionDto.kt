package com.maizeyield.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.LocalDate

// Request DTOs
data class YieldPredictionCreateRequest(
    val plantingSessionId: Long,
    val predictionDate: LocalDate,
    val predictedYieldTonsPerHectare: BigDecimal,
    val confidencePercentage: BigDecimal,
    val modelVersion: String,
    val featuresUsed: String
)

// Response DTOs
@JsonInclude(JsonInclude.Include.NON_NULL)
data class YieldPredictionResponse(
    val id: Long,
    val plantingSessionId: Long,
    val predictionDate: LocalDate,
    val predictedYieldTonsPerHectare: BigDecimal,
    val confidencePercentage: BigDecimal,
    val modelVersion: String,
    val featuresUsed: List<String>? = null,
    val predictionQuality: String? = null // e.g., "HIGH", "MEDIUM", "LOW" based on confidence
)

// Special DTOs for machine learning model
data class PredictionRequest(
    val farmId: Long,
    val plantingSessionId: Long,
    val maizeVarietyId: Long,
    val plantingDate: LocalDate,
    val currentDate: LocalDate,
    val includeSoilData: Boolean = true,
    val includeWeatherData: Boolean = true,
    val includeHistoricalYields: Boolean = true
)

data class PredictionResult(
    val predictedYieldTonsPerHectare: BigDecimal,
    val confidencePercentage: BigDecimal,
    val predictionDate: LocalDate = LocalDate.now(),
    val modelVersion: String,
    val featuresUsed: List<String>,
    val importantFactors: List<FactorImportance> = emptyList()
)

data class FactorImportance(
    val factor: String,
    val importance: BigDecimal,
    val impact: String // "POSITIVE" or "NEGATIVE"
)