package com.maizeyield.controller

import com.maizeyield.dto.RecommendationCreateRequest
import com.maizeyield.dto.RecommendationResponse
import com.maizeyield.dto.RecommendationUpdateRequest
import com.maizeyield.service.AuthService
import com.maizeyield.service.RecommendationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@Tag(name = "Recommendations", description = "Farm recommendation operations")
class RecommendationController(
    private val recommendationService: RecommendationService,
    private val authService: AuthService
) {

    @GetMapping("/planting-sessions/{sessionId}/recommendations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all recommendations for a planting session")
    fun getRecommendationsByPlantingSessionId(@PathVariable sessionId: Long): ResponseEntity<List<RecommendationResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val recommendations = recommendationService.getRecommendationsByPlantingSessionId(userId, sessionId)
        return ResponseEntity.ok(recommendations)
    }

    @GetMapping("/recommendations/{recommendationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get recommendation by ID")
    fun getRecommendationById(@PathVariable recommendationId: Long): ResponseEntity<RecommendationResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val recommendation = recommendationService.getRecommendationById(userId, recommendationId)
        return ResponseEntity.ok(recommendation)
    }

    @PostMapping("/planting-sessions/{sessionId}/recommendations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new recommendation for a planting session")
    fun createRecommendation(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: RecommendationCreateRequest
    ): ResponseEntity<RecommendationResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val recommendation = recommendationService.createRecommendation(userId, sessionId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(recommendation)
    }

    @PutMapping("/recommendations/{recommendationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update recommendation status (viewed/implemented)")
    fun updateRecommendation(
        @PathVariable recommendationId: Long,
        @Valid @RequestBody request: RecommendationUpdateRequest
    ): ResponseEntity<RecommendationResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val recommendation = recommendationService.updateRecommendation(userId, recommendationId, request)
        return ResponseEntity.ok(recommendation)
    }

    @DeleteMapping("/recommendations/{recommendationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a recommendation")
    fun deleteRecommendation(@PathVariable recommendationId: Long): ResponseEntity<Map<String, String>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        recommendationService.deleteRecommendation(userId, recommendationId)
        return ResponseEntity.ok(mapOf("message" to "Recommendation deleted successfully"))
    }

    @GetMapping("/planting-sessions/{sessionId}/recommendations/unviewed")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unviewed recommendations for a planting session")
    fun getUnviewedRecommendations(@PathVariable sessionId: Long): ResponseEntity<List<RecommendationResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val recommendations = recommendationService.getUnviewedRecommendations(userId, sessionId)
        return ResponseEntity.ok(recommendations)
    }

    @GetMapping("/planting-sessions/{sessionId}/recommendations/unimplemented")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unimplemented recommendations for a planting session")
    fun getUnimplementedRecommendations(@PathVariable sessionId: Long): ResponseEntity<List<RecommendationResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val recommendations = recommendationService.getUnimplementedRecommendations(userId, sessionId)
        return ResponseEntity.ok(recommendations)
    }

    @GetMapping("/planting-sessions/{sessionId}/recommendations/category/{category}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get recommendations by category for a planting session")
    fun getRecommendationsByCategory(
        @PathVariable sessionId: Long,
        @PathVariable category: String
    ): ResponseEntity<List<RecommendationResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val recommendations = recommendationService.getRecommendationsByCategory(userId, sessionId, category)
        return ResponseEntity.ok(recommendations)
    }

    @PostMapping("/planting-sessions/{sessionId}/recommendations/generate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate recommendations for a planting session")
    fun generateRecommendations(@PathVariable sessionId: Long): ResponseEntity<List<RecommendationResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val recommendations = recommendationService.generateRecommendations(userId, sessionId)
        return ResponseEntity.status(HttpStatus.CREATED).body(recommendations)
    }

    @GetMapping("/planting-sessions/{sessionId}/recommendations/priority/{priority}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get recommendations by priority for a planting session")
    fun getRecommendationsByPriority(
        @PathVariable sessionId: Long,
        @PathVariable priority: String
    ): ResponseEntity<List<RecommendationResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val recommendations = recommendationService.getRecommendationsByPriority(userId, sessionId, priority)
        return ResponseEntity.ok(recommendations)
    }
}