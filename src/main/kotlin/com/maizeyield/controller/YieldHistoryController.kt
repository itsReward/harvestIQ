package com.maizeyield.controller

import com.maizeyield.dto.YieldHistoryCreateRequest
import com.maizeyield.dto.YieldHistoryResponse
import com.maizeyield.dto.YieldHistoryUpdateRequest
import com.maizeyield.service.AuthService
import com.maizeyield.service.YieldHistoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate

@RestController
@RequestMapping("/api")
@Tag(name = "Yield History", description = "Yield history management operations")
class YieldHistoryController(
    private val yieldHistoryService: YieldHistoryService,
    private val authService: AuthService
) {

    @GetMapping("/planting-sessions/{sessionId}/yield-history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get yield history for a planting session")
    fun getYieldHistoryByPlantingSessionId(@PathVariable sessionId: Long): ResponseEntity<List<YieldHistoryResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val yieldHistory = yieldHistoryService.getYieldHistoryByPlantingSessionId(userId, sessionId)
        return ResponseEntity.ok(yieldHistory)
    }

    @GetMapping("/yield-history/{yieldHistoryId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get yield history by ID")
    fun getYieldHistoryById(@PathVariable yieldHistoryId: Long): ResponseEntity<YieldHistoryResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val yieldHistory = yieldHistoryService.getYieldHistoryById(userId, yieldHistoryId)
        return ResponseEntity.ok(yieldHistory)
    }

    @PostMapping("/planting-sessions/{sessionId}/yield-history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add yield history record")
    fun addYieldHistory(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: YieldHistoryCreateRequest
    ): ResponseEntity<YieldHistoryResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val yieldHistory = yieldHistoryService.addYieldHistory(userId, sessionId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(yieldHistory)
    }

    @PutMapping("/yield-history/{yieldHistoryId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update yield history record")
    fun updateYieldHistory(
        @PathVariable yieldHistoryId: Long,
        @Valid @RequestBody request: YieldHistoryUpdateRequest
    ): ResponseEntity<YieldHistoryResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val yieldHistory = yieldHistoryService.updateYieldHistory(userId, yieldHistoryId, request)
        return ResponseEntity.ok(yieldHistory)
    }

    @DeleteMapping("/yield-history/{yieldHistoryId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete yield history record")
    fun deleteYieldHistory(@PathVariable yieldHistoryId: Long): ResponseEntity<Map<String, String>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        yieldHistoryService.deleteYieldHistory(userId, yieldHistoryId)
        return ResponseEntity.ok(mapOf("message" to "Yield history deleted successfully"))
    }

    @GetMapping("/farms/{farmId}/yield-history")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all yield history for a farm")
    fun getYieldHistoryByFarmId(@PathVariable farmId: Long): ResponseEntity<List<YieldHistoryResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val yieldHistory = yieldHistoryService.getYieldHistoryByFarmId(userId, farmId)
        return ResponseEntity.ok(yieldHistory)
    }

    @GetMapping("/farms/{farmId}/yield-history/date-range")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get yield history for a farm within a date range")
    fun getYieldHistoryByDateRange(
        @PathVariable farmId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<YieldHistoryResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val yieldHistory = yieldHistoryService.getYieldHistoryByDateRange(userId, farmId, startDate, endDate)
        return ResponseEntity.ok(yieldHistory)
    }

    @GetMapping("/farms/{farmId}/yield-history/average")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Calculate average yield for a farm")
    fun calculateAverageYield(@PathVariable farmId: Long): ResponseEntity<Map<String, BigDecimal>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val averageYield = yieldHistoryService.calculateAverageYield(userId, farmId)
        return if (averageYield != null) {
            ResponseEntity.ok(mapOf("averageYield" to averageYield))
        } else {
            ResponseEntity.noContent().build()
        }
    }

    @GetMapping("/farms/{farmId}/yield-history/highest")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get highest yield record for a farm")
    fun getHighestYield(@PathVariable farmId: Long): ResponseEntity<YieldHistoryResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val highestYield = yieldHistoryService.getHighestYield(userId, farmId)
        return if (highestYield != null) {
            ResponseEntity.ok(highestYield)
        } else {
            ResponseEntity.noContent().build()
        }
    }

    @GetMapping("/planting-sessions/{sessionId}/yield-history/latest")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get latest yield record for a planting session")
    fun getLatestYieldHistory(@PathVariable sessionId: Long): ResponseEntity<YieldHistoryResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val latestYield = yieldHistoryService.getLatestYieldHistory(userId, sessionId)
        return if (latestYield != null) {
            ResponseEntity.ok(latestYield)
        } else {
            ResponseEntity.noContent().build()
        }
    }
}