package com.maizeyield.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.LocalDate

// Request DTOs
data class SoilDataCreateRequest(
    val soilType: String,
    val phLevel: BigDecimal? = null,
    val organicMatterPercentage: BigDecimal? = null,
    val nitrogenContent: BigDecimal? = null,
    val phosphorusContent: BigDecimal? = null,
    val potassiumContent: BigDecimal? = null,
    val moistureContent: BigDecimal? = null,
    val sampleDate: LocalDate
)

data class SoilDataUpdateRequest(
    val soilType: String? = null,
    val phLevel: BigDecimal? = null,
    val organicMatterPercentage: BigDecimal? = null,
    val nitrogenContent: BigDecimal? = null,
    val phosphorusContent: BigDecimal? = null,
    val potassiumContent: BigDecimal? = null,
    val moistureContent: BigDecimal? = null,
    val sampleDate: LocalDate? = null
)

// Response DTOs
@JsonInclude(JsonInclude.Include.NON_NULL)
data class SoilDataResponse(
    val id: Long,
    val farmId: Long,
    val soilType: String,
    val phLevel: BigDecimal? = null,
    val organicMatterPercentage: BigDecimal? = null,
    val nitrogenContent: BigDecimal? = null,
    val phosphorusContent: BigDecimal? = null,
    val potassiumContent: BigDecimal? = null,
    val moistureContent: BigDecimal? = null,
    val sampleDate: LocalDate,
    val soilHealthScore: Int? = null,
    val fertilizerRecommendation: String? = null
)