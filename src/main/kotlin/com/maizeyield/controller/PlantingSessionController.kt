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
@RequestMapping("/api/planting-sessions")
@Tag(name = "Planting Sessions", description = "Planting session management operations")
class PlantingSessionController(
    private val plantingSessionService: PlantingSessionService,
    private val authService: AuthService
) {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all planting sessions")
    fun getAllPlantingSessions(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "") search: String
    ): ResponseEntity<Page<PlantingSessionResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val sessions = plantingSessionService.getAllPlantingSessions(userId, pageable, search)
        return ResponseEntity.ok(sessions)
    }

    @GetMapping("/farms/{farmId}")
    @PreAuthorize("hasRole('ADMIN') or @farmService.isFarmOwner(authentication.principal.id, #farmId)")
    @Operation(summary = "Get all planting sessions for a farm")
    fun getPlantingSessionsByFarmId(@PathVariable farmId: Long): ResponseEntity<List<PlantingSessionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val sessions = plantingSessionService.getPlantingSessionsByFarmId(userId, farmId)
        return ResponseEntity.ok(sessions)
    }

    @GetMapping("/{sessionId}")
    @PreAuthorize("hasRole('ADMIN') or @plantingSessionService.isSessionOwner(authentication.principal.id, #sessionId)")
    @Operation(summary = "Get planting session details by ID")
    fun getPlantingSessionById(@PathVariable sessionId: Long): ResponseEntity<PlantingSessionDetailResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val session = plantingSessionService.getPlantingSessionById(userId, sessionId)
        return ResponseEntity.ok(session)
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new planting session")
    fun createPlantingSession(
        @Valid @RequestBody request: PlantingSessionCreateRequest
    ): ResponseEntity<PlantingSessionResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val session = plantingSessionService.createPlantingSession(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(session)
    }

    @PutMapping("/{sessionId}")
    @PreAuthorize("hasRole('ADMIN') or @plantingSessionService.isSessionOwner(authentication.principal.id, #sessionId)")
    @Operation(summary = "Update planting session")
    fun updatePlantingSession(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: PlantingSessionUpdateRequest
    ): ResponseEntity<PlantingSessionResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val session = plantingSessionService.updatePlantingSession(userId, sessionId, request)
        return ResponseEntity.ok(session)
    }

    @DeleteMapping("/{sessionId}")
    @PreAuthorize("hasRole('ADMIN') or @plantingSessionService.isSessionOwner(authentication.principal.id, #sessionId)")
    @Operation(summary = "Delete planting session")
    fun deletePlantingSession(@PathVariable sessionId: Long): ResponseEntity<Map<String, String>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        plantingSessionService.deletePlantingSession(userId, sessionId)
        return ResponseEntity.ok(mapOf("message" to "Planting session deleted successfully"))
    }

    @GetMapping("/{sessionId}/status")
    @PreAuthorize("hasRole('ADMIN') or @plantingSessionService.isSessionOwner(authentication.principal.id, #sessionId)")
    @Operation(summary = "Get planting session status")
    fun getPlantingSessionStatus(@PathVariable sessionId: Long): ResponseEntity<Map<String, Any>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val status = plantingSessionService.getPlantingSessionStatus(userId, sessionId)
        return ResponseEntity.ok(status)
    }

    @PostMapping("/{sessionId}/status")
    @PreAuthorize("hasRole('ADMIN') or @plantingSessionService.isSessionOwner(authentication.principal.id, #sessionId)")
    @Operation(summary = "Update planting session status")
    fun updatePlantingSessionStatus(
        @PathVariable sessionId: Long,
        @RequestParam status: String
    ): ResponseEntity<Map<String, String>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        plantingSessionService.updatePlantingSessionStatus(userId, sessionId, status)
        return ResponseEntity.ok(mapOf("message" to "Planting session status updated successfully"))
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all active planting sessions")
    fun getActivePlantingSessions(): ResponseEntity<List<PlantingSessionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val sessions = plantingSessionService.getActivePlantingSessions(userId)
        return ResponseEntity.ok(sessions)
    }

    @GetMapping("/date-range")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get planting sessions by date range")
    fun getPlantingSessionsByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<PlantingSessionResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val sessions = plantingSessionService.getPlantingSessionsByDateRange(userId, startDate, endDate)
        return ResponseEntity.ok(sessions)
    }
}