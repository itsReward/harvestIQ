package com.maizeyield.controller

import com.maizeyield.dto.FarmCreateRequest
import com.maizeyield.dto.FarmDetailResponse
import com.maizeyield.dto.FarmResponse
import com.maizeyield.dto.FarmUpdateRequest
import com.maizeyield.service.AuthService
import com.maizeyield.service.FarmService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/farms")
@Tag(name = "Farms", description = "Farm management operations")
class FarmController(
    private val farmService: FarmService,
    private val authService: AuthService
) {

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all farms for the current user")
    fun getAllFarms(): ResponseEntity<List<FarmResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val farms = farmService.getFarmsByUserId(userId)
        return ResponseEntity.ok(farms)
    }

    @GetMapping("/{farmId}")
    @PreAuthorize("isAuthenticated() && @farmService.isFarmOwner(#userId, #farmId)")
    @Operation(summary = "Get farm details by ID")
    fun getFarmById(@PathVariable farmId: Long): ResponseEntity<FarmDetailResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val farm = farmService.getFarmById(userId, farmId)
        return ResponseEntity.ok(farm)
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new farm")
    fun createFarm(@Valid @RequestBody request: FarmCreateRequest): ResponseEntity<FarmResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val farm = farmService.createFarm(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(farm)
    }

    @PutMapping("/{farmId}")
    @PreAuthorize("isAuthenticated() && @farmService.isFarmOwner(#userId, #farmId)")
    @Operation(summary = "Update farm details")
    fun updateFarm(
        @PathVariable farmId: Long,
        @Valid @RequestBody request: FarmUpdateRequest
    ): ResponseEntity<FarmResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val farm = farmService.updateFarm(userId, farmId, request)
        return ResponseEntity.ok(farm)
    }

    @DeleteMapping("/{farmId}")
    @PreAuthorize("isAuthenticated() && @farmService.isFarmOwner(#userId, #farmId)")
    @Operation(summary = "Delete a farm")
    fun deleteFarm(@PathVariable farmId: Long): ResponseEntity<Map<String, String>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        farmService.deleteFarm(userId, farmId)
        return ResponseEntity.ok(mapOf("message" to "Farm deleted successfully"))
    }
}