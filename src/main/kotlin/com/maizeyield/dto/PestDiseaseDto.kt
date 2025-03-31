package com.maizeyield.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.LocalDate

// Request DTOs
data class PestsDiseaseCreateRequest(
    val plantingSessionId: Long,
    val observationDate: LocalDate,
    val pestOrDiseaseName: String,
    val severityLevel: String,
    val affectedAreaPercentage: BigDecimal? = null,
    val treatmentApplied: String? = null,
    val notes: String? = null
)

data class PestsDiseaseUpdateRequest(
    val severityLevel: String? = null,
    val affectedAreaPercentage: BigDecimal? = null,
    val treatmentApplied: String? = null,
    val notes: String? = null
)

// Response DTOs
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PestsDiseaseResponse(
    val id: Long,
    val plantingSessionId: Long,
    val observationDate: LocalDate,
    val pestOrDiseaseName: String,
    val severityLevel: String,
    val affectedAreaPercentage: BigDecimal? = null,
    val treatmentApplied: String? = null,
    val notes: String? = null,
    val recommendedTreatments: List<String>? = null
)