package com.maizeyield.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.LocalDateTime

// Request DTOs
data class FarmCreateRequest(
    val name: String,
    val location: String,
    val sizeHectares: BigDecimal,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val elevation: BigDecimal? = null
)

data class FarmUpdateRequest(
    val name: String? = null,
    val location: String? = null,
    val sizeHectares: BigDecimal? = null,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val elevation: BigDecimal? = null
)

// Response DTOs
@JsonInclude(JsonInclude.Include.NON_NULL)
data class FarmResponse(
    val id: Long,
    val name: String,
    val location: String,
    val sizeHectares: BigDecimal,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val elevation: BigDecimal? = null,
    val createdAt: LocalDateTime,
    val ownerId: Long? = null,
    val ownerName: String? = null,
    val ownerEmail: String? = null,
    val ownerPhone: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FarmDetailResponse(
    val id: Long,
    val name: String,
    val location: String,
    val sizeHectares: BigDecimal,
    val latitude: BigDecimal? = null,
    val longitude: BigDecimal? = null,
    val elevation: BigDecimal? = null,
    val createdAt: LocalDateTime,
    // ADDED: Owner information and contact details
    val ownerId: Long? = null,
    val ownerName: String? = null,
    val ownerEmail: String? = null,
    val ownerPhone: String? = null,
    val soilData: SoilDataResponse? = null,
    val activePlantingSessions: List<PlantingSessionResponse> = emptyList()
)