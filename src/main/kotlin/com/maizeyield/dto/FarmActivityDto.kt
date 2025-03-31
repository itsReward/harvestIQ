package com.maizeyield.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.LocalDate

// Request DTOs
data class FarmActivityCreateRequest(
    val plantingSessionId: Long,
    val activityDate: LocalDate,
    val activityType: String,
    val description: String,
    val laborHours: BigDecimal? = null,
    val cost: BigDecimal? = null,
    val notes: String? = null
)

data class FarmActivityUpdateRequest(
    val activityDate: LocalDate? = null,
    val activityType: String? = null,
    val description: String? = null,
    val laborHours: BigDecimal? = null,
    val cost: BigDecimal? = null,
    val notes: String? = null
)

// Response DTOs
@JsonInclude(JsonInclude.Include.NON_NULL)
data class FarmActivityResponse(
    val id: Long,
    val plantingSessionId: Long,
    val activityDate: LocalDate,
    val activityType: String,
    val description: String,
    val laborHours: BigDecimal? = null,
    val cost: BigDecimal? = null,
    val notes: String? = null
)