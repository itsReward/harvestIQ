package com.maizeyield.controller

import com.maizeyield.dto.FactorImportance
import com.maizeyield.dto.YieldPredictionResponse
import com.maizeyield.service.AuthService
import com.maizeyield.service.PredictionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/predictions")
@Tag(name = "Predictions", description = "Yield prediction operations")
class PredictionController(
    private val predictionService: PredictionService,
    private val authService: AuthService
) {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all predictions")
    fun getAllPredictions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) sessionId: Long?
    ): ResponseEntity<Iterable<YieldPredictionResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val predictions = predictionService.getAllPredictions(userId, pageable, sessionId)
        return ResponseEntity.ok(predictions)
    }

    @PostMapping("/planting-sessions/{sessionId}")
    @PreAuthorize("hasRole('ADMIN') or @plantingSessionService.isSessionOwner(authentication.principal.id, #sessionId)")
    @Operation(summary = "Generate a yield prediction for a planting session")
    fun generatePrediction(@PathVariable sessionId: Long): ResponseEntity<YieldPredictionResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val prediction = predictionService.generatePrediction(userId, sessionId)
        return ResponseEntity.status(HttpStatus.CREATED).body(prediction)
    }

    @GetMapping("/planting-sessions/{sessionId}")
    @PreAuthorize("hasRole('ADMIN') or @plantingSessionService.isSessionOwner(authentication.principal.id, #sessionId)")
    @Operation(summary = "Get all predictions for a planting session")
    fun getPredictionsByPlantingSessionId(@PathVariable sessionId: Long): ResponseEntity<List<YieldPredictionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val predictions = predictionService.getPredictionsByPlantingSessionId(userId, sessionId)
        return ResponseEntity.ok(predictions)
    }

    @GetMapping("/{predictionId}")
    @PreAuthorize("hasRole('ADMIN') or @predictionService.isPredictionOwner(authentication.principal.id, #predictionId)")
    @Operation(summary = "Get prediction by ID")
    fun getPredictionById(@PathVariable predictionId: Long): ResponseEntity<YieldPredictionResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val prediction = predictionService.getPredictionById(userId, predictionId)
        return ResponseEntity.ok(prediction)
    }

    @GetMapping("/planting-sessions/{sessionId}/latest")
    @PreAuthorize("hasRole('ADMIN') or @plantingSessionService.isSessionOwner(authentication.principal.id, #sessionId)")
    @Operation(summary = "Get latest prediction for a planting session")
    fun getLatestPrediction(@PathVariable sessionId: Long): ResponseEntity<YieldPredictionResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val prediction = predictionService.getLatestPrediction(userId, sessionId)
        return if (prediction != null) {
            ResponseEntity.ok(prediction)
        } else {
            ResponseEntity.noContent().build()
        }
    }

    @GetMapping("/farms/{farmId}")
    @PreAuthorize("hasRole('ADMIN') or @farmService.isFarmOwner(authentication.principal.id, #farmId)")
    @Operation(summary = "Get predictions for all active planting sessions of a farm")
    fun getPredictionsForFarm(@PathVariable farmId: Long): ResponseEntity<List<YieldPredictionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val predictions = predictionService.getPredictionsForFarm(userId, farmId)
        return ResponseEntity.ok(predictions)
    }

    @PostMapping("/farms/{farmId}/batch")
    @PreAuthorize("hasRole('ADMIN') or @farmService.isFarmOwner(authentication.principal.id, #farmId)")
    @Operation(summary = "Generate predictions for all active planting sessions of a farm")
    fun generatePredictionsForFarm(@PathVariable farmId: Long): ResponseEntity<List<YieldPredictionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val predictions = predictionService.generatePredictionsForFarm(userId, farmId)
        return ResponseEntity.status(HttpStatus.CREATED).body(predictions)
    }

    @GetMapping("/{predictionId}/factors")
    @PreAuthorize("hasRole('ADMIN') or @predictionService.isPredictionOwner(authentication.principal.id, #predictionId)")
    @Operation(summary = "Get the important factors affecting yield for a specific prediction")
    fun getImportantFactors(@PathVariable predictionId: Long): ResponseEntity<List<FactorImportance>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val factors = predictionService.getImportantFactors(userId, predictionId)
        return ResponseEntity.ok(factors)
    }

    @GetMapping("/planting-sessions/{sessionId}/history")
    @PreAuthorize("hasRole('ADMIN') or @plantingSessionService.isSessionOwner(authentication.principal.id, #sessionId)")
    @Operation(summary = "Get prediction history for a planting session")
    fun getPredictionHistory(
        @PathVariable sessionId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<YieldPredictionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val predictions = predictionService.getPredictionHistory(userId, sessionId, startDate, endDate)
        return ResponseEntity.ok(predictions)
    }

    @DeleteMapping("/{predictionId}")
    @PreAuthorize("hasRole('ADMIN') or @predictionService.isPredictionOwner(authentication.principal.id, #predictionId)")
    @Operation(summary = "Delete a prediction")
    fun deletePrediction(@PathVariable predictionId: Long): ResponseEntity<Map<String, String>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        predictionService.deletePrediction(userId, predictionId)
        return ResponseEntity.ok(mapOf("message" to "Prediction deleted successfully"))
    }

    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get recent predictions")
    fun getRecentPredictions(
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<YieldPredictionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val predictions = predictionService.getRecentPredictions(userId, limit)
        return ResponseEntity.ok(predictions)
    }

    @GetMapping("/accuracy-metrics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get prediction accuracy metrics (Admin only)")
    fun getPredictionAccuracyMetrics(): ResponseEntity<Map<String, Any>> {
        val metrics = predictionService.getPredictionAccuracyMetrics()
        return ResponseEntity.ok(metrics)
    }
}