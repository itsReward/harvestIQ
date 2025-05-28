package com.maizeyield.controller

import com.maizeyield.dto.MaizeVarietyCreateRequest
import com.maizeyield.dto.MaizeVarietyResponse
import com.maizeyield.dto.MaizeVarietyUpdateRequest
import com.maizeyield.service.MaizeVarietyService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/maize-varieties")
@Tag(name = "Maize Varieties", description = "Maize variety management operations")
class MaizeVarietyController(private val maizeVarietyService: MaizeVarietyService) {

    @GetMapping
    @Operation(summary = "Get all available maize varieties")
    fun getAllMaizeVarieties(): ResponseEntity<List<MaizeVarietyResponse>> {
        val varieties = maizeVarietyService.getAllMaizeVarieties()
        return ResponseEntity.ok(varieties)
    }

    @GetMapping("/{varietyId}")
    @Operation(summary = "Get maize variety by ID")
    fun getMaizeVarietyById(@PathVariable varietyId: Long): ResponseEntity<MaizeVarietyResponse> {
        val variety = maizeVarietyService.getMaizeVarietyById(varietyId)
        return ResponseEntity.ok(variety)
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new maize variety (Admin only)")
    fun createMaizeVariety(@Valid @RequestBody request: MaizeVarietyCreateRequest): ResponseEntity<MaizeVarietyResponse> {
        val variety = maizeVarietyService.createMaizeVariety(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(variety)
    }

    @PutMapping("/{varietyId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update maize variety (Admin only)")
    fun updateMaizeVariety(
        @PathVariable varietyId: Long,
        @Valid @RequestBody request: MaizeVarietyUpdateRequest
    ): ResponseEntity<MaizeVarietyResponse> {
        val variety = maizeVarietyService.updateMaizeVariety(varietyId, request)
        return ResponseEntity.ok(variety)
    }

    @DeleteMapping("/{varietyId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete maize variety (Admin only)")
    fun deleteMaizeVariety(@PathVariable varietyId: Long): ResponseEntity<Map<String, String>> {
        maizeVarietyService.deleteMaizeVariety(varietyId)
        return ResponseEntity.ok(mapOf("message" to "Maize variety deleted successfully"))
    }

    @GetMapping("/drought-resistant")
    @Operation(summary = "Get drought-resistant maize varieties")
    fun getDroughtResistantVarieties(): ResponseEntity<List<MaizeVarietyResponse>> {
        val varieties = maizeVarietyService.getDroughtResistantVarieties()
        return ResponseEntity.ok(varieties)
    }

    @GetMapping("/maturity-range")
    @Operation(summary = "Get maize varieties by maturity days range")
    fun getVarietiesByMaturityRange(
        @RequestParam minDays: Int,
        @RequestParam maxDays: Int
    ): ResponseEntity<List<MaizeVarietyResponse>> {
        val varieties = maizeVarietyService.getVarietiesByMaturityRange(minDays, maxDays)
        return ResponseEntity.ok(varieties)
    }
}