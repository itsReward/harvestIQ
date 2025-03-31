package com.maizeyield.controller

import com.maizeyield.dto.PlantingSessionCreateRequest
import com.maizeyield.dto.PlantingSessionDetailResponse
import com.maizeyield.dto.PlantingSessionResponse
import com.maizeyield.dto.PlantingSessionUpdateRequest
import com.maizeyield.service.AuthService
import com.maizeyield.service.PlantingSessionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api")
@Tag(name = "Planting Sessions", description = "Planting session management operations")
class PlantingSessionController(
    private val plantingSessionService: PlantingSessionService,
    private val authService: AuthService
) {

    @GetMapping("/farms/{farmId}/planting-sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all planting sessions for a farm")
    fun getPlantingSessionsByFarmId(@PathVariable farmId: Long): ResponseEntity<List<PlantingSessionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val sessions = plantingSessionService.getPlantingSessionsByFarmId(userId, farmId)
        return ResponseEntity.ok(sessions)
    }

    @GetMapping("/planting-sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get planting session details by ID")
    fun getPlantingSessionById(@PathVariable sessionId: Long): ResponseEntity<PlantingSessionDetailResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val session = plantingSessionService.getPlantingSessionById(userId, sessionId)
        return ResponseEntity.ok(session)
    }

    @PostMapping("/farms/{farmId}/planting-sessions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new planting session")
    fun createPlantingSession(
        @PathVariable farmId: Long,
        @Valid @RequestBody request: PlantingSessionCreateRequest
    ): ResponseEntity<PlantingSessionResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val session = plantingSessionService.createPlantingSession(userId, farmId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(session)
    }

    @PutMapping("/planting-sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update planting session details")
    fun updatePlantingSession(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: PlantingSessionUpdateRequest
    ): ResponseEntity<PlantingSessionResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val session = plantingSessionService.updatePlantingSession(userId, sessionId, request)
        return ResponseEntity.ok(session)
    }

    @DeleteMapping("/planting-sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a planting session")
    fun deletePlantingSession(@PathVariable sessionId: Long): ResponseEntity<Map<String, String>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        plantingSessionService.deletePlantingSession(userId, sessionId)
        return ResponseEntity.ok(mapOf("message" to "Planting session deleted successfully"))
    }

    @GetMapping("/farms/{farmId}/planting-sessions/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get active planting sessions for a farm")
    fun getActivePlantingSessions(@PathVariable farmId: Long): ResponseEntity<List<PlantingSessionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val sessions = plantingSessionService.getActivePlantingSessions(userId, farmId)
        return ResponseEntity.ok(sessions)
    }

    @GetMapping("/farms/{farmId}/planting-sessions/date-range")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get planting sessions for a specific date range")
    fun getPlantingSessionsByDateRange(
        @PathVariable farmId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<PlantingSessionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val sessions = plantingSessionService.getPlantingSessionsByDateRange(userId, farmId, startDate, endDate)
        return ResponseEntity.ok(sessions)
    }

    @GetMapping("/maize-varieties/{maizeVarietyId}/expected-harvest")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Calculate expected harvest date based on maize variety and planting date")
    fun calculateExpectedHarvestDate(
        @PathVariable maizeVarietyId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) plantingDate: LocalDate
    ): ResponseEntity<Map<String, LocalDate>> {
        val harvestDate = plantingSessionService.calculateExpectedHarvestDate(maizeVarietyId, plantingDate)
        return ResponseEntity.ok(mapOf("expectedHarvestDate" to harvestDate))
    }
}