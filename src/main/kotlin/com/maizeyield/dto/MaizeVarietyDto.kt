package com.maizeyield.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal

// Request DTOs
data class MaizeVarietyCreateRequest(
    val name: String,
    val maturityDays: Int,
    val optimalTemperatureMin: BigDecimal? = null,
    val optimalTemperatureMax: BigDecimal? = null,
    val droughtResistance: Boolean = false,
    val diseaseResistance: String? = null,
    val description: String? = null
)

data class MaizeVarietyUpdateRequest(
    val name: String? = null,
    val maturityDays: Int? = null,
    val optimalTemperatureMin: BigDecimal? = null,
    val optimalTemperatureMax: BigDecimal? = null,
    val droughtResistance: Boolean? = null,
    val diseaseResistance: String? = null,
    val description: String? = null
)

// Response DTOs
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MaizeVarietyResponse(
    val id: Long,
    val name: String,
    val maturityDays: Int,
    val optimalTemperatureMin: BigDecimal? = null,
    val optimalTemperatureMax: BigDecimal? = null,
    val droughtResistance: Boolean,
    val diseaseResistance: String? = null,
    val description: String? = null
)