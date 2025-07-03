package com.maizeyield.controller

import com.maizeyield.dto.FarmCreateRequest
import com.maizeyield.dto.FarmDetailResponse
import com.maizeyield.dto.FarmResponse
import com.maizeyield.dto.FarmUpdateRequest
import com.maizeyield.repository.UserRepository
import com.maizeyield.service.AuthService
import com.maizeyield.service.FarmService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/farms")
@Tag(name = "Farms", description = "Farm management operations")
class FarmController(
    private val farmService: FarmService,
    private val authService: AuthService,
    private val userRepository: UserRepository,
) {


    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all farms")
    fun getAllFarms(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "") search: String
    ): ResponseEntity<Page<FarmResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())

        // Check if user is admin - if so, show ALL farms
        val user = userRepository.findById(userId)
            .orElseThrow { RuntimeException("User not found") }

        val farms = if (user.role == "ADMIN") {
            // Admin sees ALL farms in the system
            farmService.getAllFarmsForAdmin(pageable, search)
        } else {
            // Regular users see only their own farms
            farmService.getAllFarms(userId, pageable, search)
        }

        return ResponseEntity.ok(farms)
    }

    // Rest of your existing methods remain the same...
    @GetMapping("/my-farms")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's farms")
    fun getMyFarms(): ResponseEntity<List<FarmResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val farms = farmService.getFarmsByUserId(userId)
        return ResponseEntity.ok(farms)
    }


    @GetMapping("/{farmId}")
    //@PreAuthorize("hasRole('ADMIN') or @farmService.isFarmOwner(authentication.principal.id, #farmId)")
    @PreAuthorize("isAuthenticated()")
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
    //@PreAuthorize("hasRole('ADMIN') or @farmService.isFarmOwner(authentication.principal.id, #farmId)")
    @PreAuthorize("isAuthenticated()")
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
    //@PreAuthorize("hasRole('ADMIN') or @farmService.isFarmOwner(authentication.principal.id, #farmId)")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a farm")
    fun deleteFarm(@PathVariable farmId: Long): ResponseEntity<Map<String, String>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        farmService.deleteFarm(userId, farmId)
        return ResponseEntity.ok(mapOf("message" to "Farm deleted successfully"))
    }

    @GetMapping("/{farmId}/statistics")
    //@PreAuthorize("hasRole('ADMIN') or @farmService.isFarmOwner(authentication.principal.id, #farmId)")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get farm statistics")
    fun getFarmStatistics(@PathVariable farmId: Long): ResponseEntity<Map<String, Any>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val statistics = farmService.getFarmStatistics(userId, farmId)
        return ResponseEntity.ok(statistics)
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    @Operation(summary = "Get farms by user ID")
    fun getFarmsByUserId(@PathVariable userId: Long): ResponseEntity<List<FarmResponse>> {
        val farms = farmService.getFarmsByUserId(userId)
        return ResponseEntity.ok(farms)
    }

    @PostMapping("/{farmId}/transfer")
    //@PreAuthorize("hasRole('ADMIN') or @farmService.isFarmOwner(authentication.principal.id, #farmId)")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Transfer farm ownership")
    fun transferFarmOwnership(
        @PathVariable farmId: Long,
        @RequestParam newOwnerId: Long
    ): ResponseEntity<Map<String, String>> {
        val currentUserId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        farmService.transferFarmOwnership(currentUserId, farmId, newOwnerId)
        return ResponseEntity.ok(mapOf("message" to "Farm ownership transferred successfully"))
    }

    @GetMapping("/region/{region}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get farms by region (Admin only)")
    fun getFarmsByRegion(@PathVariable region: String): ResponseEntity<List<FarmResponse>> {
        val farms = farmService.getFarmsByRegion(region)
        return ResponseEntity.ok(farms)
    }

    @GetMapping("/statistics/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get farm statistics summary (Admin only)")
    fun getFarmStatisticsSummary(): ResponseEntity<Map<String, Any>> {
        val summary = farmService.getFarmStatisticsSummary()
        return ResponseEntity.ok(summary)
    }
}