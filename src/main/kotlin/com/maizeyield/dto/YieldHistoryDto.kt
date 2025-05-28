package com.maizeyield.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.LocalDate

// Request DTOs
data class YieldHistoryCreateRequest(
    val harvestDate: LocalDate,
    val yieldTonsPerHectare: BigDecimal,
    val qualityRating: String? = null,
    val notes: String? = null
)

data class YieldHistoryUpdateRequest(
    val harvestDate: LocalDate? = null,
    val yieldTonsPerHectare: BigDecimal? = null,
    val qualityRating: String? = null,
    val notes: String? = null
)

// Response DTOs
@JsonInclude(JsonInclude.Include.NON_NULL)
data class YieldHistoryResponse(
    val id: Long,
    val plantingSessionId: Long,
    val harvestDate: LocalDate,
    val yieldTonsPerHectare: BigDecimal,
    val qualityRating: String? = null,
    val notes: String? = null,
    val farmName: String? = null,
    val maizeVarietyName: String? = null,
    val plantingDate: LocalDate? = null,
    val daysToMaturity: Int? = null
)