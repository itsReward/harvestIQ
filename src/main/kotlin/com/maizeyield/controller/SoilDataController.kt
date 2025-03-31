package com.maizeyield.controller

import com.maizeyield.dto.SoilDataCreateRequest
import com.maizeyield.dto.SoilDataResponse
import com.maizeyield.dto.SoilDataUpdateRequest
import com.maizeyield.service.AuthService
import com.maizeyield.service.SoilDataService
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
@Tag(name = "Soil Data", description = "Soil data management operations")
class SoilDataController(
    private val soilDataService: SoilDataService,
    private val authService: AuthService
) {

    @GetMapping("/farms/{farmId}/soil-data")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all soil data for a farm")
    fun getSoilDataByFarmId(@PathVariable farmId: Long): ResponseEntity<List<SoilDataResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val soilDataList = soilDataService.getSoilDataByFarmId(userId, farmId)
        return ResponseEntity.ok(soilDataList)
    }

    @GetMapping("/soil-data/{soilDataId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get soil data by ID")
    fun getSoilDataById(@PathVariable soilDataId: Long): ResponseEntity<SoilDataResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val soilData = soilDataService.getSoilDataById(userId, soilDataId)
        return ResponseEntity.ok(soilData)
    }

    @PostMapping("/farms/{farmId}/soil-data")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add soil data to a farm")
    fun addSoilData(
        @PathVariable farmId: Long,
        @Valid @RequestBody request: SoilDataCreateRequest
    ): ResponseEntity<SoilDataResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val soilData = soilDataService.addSoilData(userId, farmId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(soilData)
    }

    @PutMapping("/soil-data/{soilDataId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update soil data")
    fun updateSoilData(
        @PathVariable soilDataId: Long,
        @Valid @RequestBody request: SoilDataUpdateRequest
    ): ResponseEntity<SoilDataResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val soilData = soilDataService.updateSoilData(userId, soilDataId, request)
        return ResponseEntity.ok(soilData)
    }

    @DeleteMapping("/soil-data/{soilDataId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete soil data")
    fun deleteSoilData(@PathVariable soilDataId: Long): ResponseEntity<Map<String, String>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        soilDataService.deleteSoilData(userId, soilDataId)
        return ResponseEntity.ok(mapOf("message" to "Soil data deleted successfully"))
    }

    @GetMapping("/farms/{farmId}/soil-data/latest")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get latest soil data for a farm")
    fun getLatestSoilData(@PathVariable farmId: Long): ResponseEntity<SoilDataResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val soilData = soilDataService.getLatestSoilData(userId, farmId)
        return if (soilData != null) {
            ResponseEntity.ok(soilData)
        } else {
            ResponseEntity.noContent().build()
        }
    }

    @GetMapping("/farms/{farmId}/soil-data/date-range")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get soil data for a specific date range")
    fun getSoilDataByDateRange(
        @PathVariable farmId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<SoilDataResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val soilDataList = soilDataService.getSoilDataByDateRange(userId, farmId, startDate, endDate)
        return ResponseEntity.ok(soilDataList)
    }
}