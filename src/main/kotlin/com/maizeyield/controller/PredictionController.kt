package com.maizeyield.controller

import com.maizeyield.dto.FactorImportance
import com.maizeyield.dto.YieldPredictionResponse
import com.maizeyield.service.AuthService
import com.maizeyield.service.PredictionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api")
@Tag(name = "Predictions", description = "Yield prediction operations")
class PredictionController(
    private val predictionService: PredictionService,
    private val authService: AuthService
) {

    @PostMapping("/planting-sessions/{sessionId}/predictions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate a yield prediction for a planting session")
    fun generatePrediction(@PathVariable sessionId: Long): ResponseEntity<YieldPredictionResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val prediction = predictionService.generatePrediction(userId, sessionId)
        return ResponseEntity.status(HttpStatus.CREATED).body(prediction)
    }

    @GetMapping("/planting-sessions/{sessionId}/predictions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all predictions for a planting session")
    fun getPredictionsByPlantingSessionId(@PathVariable sessionId: Long): ResponseEntity<List<YieldPredictionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val predictions = predictionService.getPredictionsByPlantingSessionId(userId, sessionId)
        return ResponseEntity.ok(predictions)
    }

    @GetMapping("/predictions/{predictionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get prediction by ID")
    fun getPredictionById(@PathVariable predictionId: Long): ResponseEntity<YieldPredictionResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val prediction = predictionService.getPredictionById(userId, predictionId)
        return ResponseEntity.ok(prediction)
    }

    @GetMapping("/planting-sessions/{sessionId}/predictions/latest")
    @PreAuthorize("isAuthenticated()")
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

    @GetMapping("/farms/{farmId}/predictions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get predictions for all active planting sessions of a farm")
    fun getPredictionsForFarm(@PathVariable farmId: Long): ResponseEntity<List<YieldPredictionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val predictions = predictionService.getPredictionsForFarm(userId, farmId)
        return ResponseEntity.ok(predictions)
    }

    @PostMapping("/farms/{farmId}/predictions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Generate predictions for all active planting sessions of a farm")
    fun generatePredictionsForFarm(@PathVariable farmId: Long): ResponseEntity<List<YieldPredictionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val predictions = predictionService.generatePredictionsForFarm(userId, farmId)
        return ResponseEntity.status(HttpStatus.CREATED).body(predictions)
    }

    @GetMapping("/predictions/{predictionId}/factors")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the important factors affecting yield for a specific prediction")
    fun getImportantFactors(@PathVariable predictionId: Long): ResponseEntity<List<FactorImportance>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val factors = predictionService.getImportantFactors(userId, predictionId)
        return ResponseEntity.ok(factors)
    }

    @GetMapping("/planting-sessions/{sessionId}/predictions/history")
    @PreAuthorize("isAuthenticated()")
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
}