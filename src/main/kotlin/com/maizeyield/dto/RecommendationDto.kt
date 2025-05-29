package com.maizeyield.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

// Request DTOs
data class RecommendationCreateRequest(
    val plantingSessionId: Long,
    val category: String,
    val title: String,
    val description: String,
    val priority: String,
    val recommendationDate: LocalDate = LocalDate.now()
)

data class RecommendationUpdateRequest(
    val isViewed: Boolean? = null,
    val isImplemented: Boolean? = null
)

// Response DTOs
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RecommendationResponse(
    val id: Long,
    val plantingSessionId: Long,
    val category: String,
    val title: String,
    val description: String,
    val priority: String,
    val recommendationDate: LocalDate,
    val isViewed: Boolean,
    val isImplemented: Boolean,
    val confidence: Float,
)

// Special DTOs for recommendation generation
data class RecommendationGenerationRequest(
    val plantingSessionId: Long,
    val currentDate: LocalDate = LocalDate.now(),
    val recommendationTypes: List<String> = listOf("IRRIGATION", "FERTILIZATION", "PEST_CONTROL", "GENERAL")
)

data class GeneratedRecommendations(
    val recommendations: List<RecommendationCreateRequest>
)