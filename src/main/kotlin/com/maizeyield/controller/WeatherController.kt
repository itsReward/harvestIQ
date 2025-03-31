package com.maizeyield.controller

import com.maizeyield.dto.WeatherDataCreateRequest
import com.maizeyield.dto.WeatherDataResponse
import com.maizeyield.dto.WeatherDataUpdateRequest
import com.maizeyield.dto.WeatherForecastResponse
import com.maizeyield.service.AuthService
import com.maizeyield.service.WeatherService
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
@Tag(name = "Weather", description = "Weather data operations")
class WeatherController(
    private val weatherService: WeatherService,
    private val authService: AuthService
) {

    @GetMapping("/farms/{farmId}/weather")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all weather data for a farm")
    fun getWeatherDataByFarmId(@PathVariable farmId: Long): ResponseEntity<List<WeatherDataResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val weatherDataList = weatherService.getWeatherDataByFarmId(userId, farmId)
        return ResponseEntity.ok(weatherDataList)
    }

    @GetMapping("/farms/{farmId}/weather/date-range")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get weather data for a specific date range")
    fun getWeatherDataByDateRange(
        @PathVariable farmId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<List<WeatherDataResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val weatherDataList = weatherService.getWeatherDataByDateRange(userId, farmId, startDate, endDate)
        return ResponseEntity.ok(weatherDataList)
    }

    @GetMapping("/weather/{weatherDataId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get weather data by ID")
    fun getWeatherDataById(@PathVariable weatherDataId: Long): ResponseEntity<WeatherDataResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val weatherData = weatherService.getWeatherDataById(userId, weatherDataId)
        return ResponseEntity.ok(weatherData)
    }

    @PostMapping("/weather")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add weather data")
    fun addWeatherData(@Valid @RequestBody request: WeatherDataCreateRequest): ResponseEntity<WeatherDataResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val weatherData = weatherService.addWeatherData(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(weatherData)
    }

    @PutMapping("/weather/{weatherDataId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update weather data")
    fun updateWeatherData(
        @PathVariable weatherDataId: Long,
        @Valid @RequestBody request: WeatherDataUpdateRequest
    ): ResponseEntity<WeatherDataResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val weatherData = weatherService.updateWeatherData(userId, weatherDataId, request)
        return ResponseEntity.ok(weatherData)
    }

    @DeleteMapping("/weather/{weatherDataId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete weather data")
    fun deleteWeatherData(@PathVariable weatherDataId: Long): ResponseEntity<Map<String, String>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        weatherService.deleteWeatherData(userId, weatherDataId)
        return ResponseEntity.ok(mapOf("message" to "Weather data deleted successfully"))
    }

    @GetMapping("/farms/{farmId}/weather/latest")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get latest weather data for a farm")
    fun getLatestWeatherData(@PathVariable farmId: Long): ResponseEntity<WeatherDataResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val weatherData = weatherService.getLatestWeatherData(userId, farmId)
        return if (weatherData != null) {
            ResponseEntity.ok(weatherData)
        } else {
            ResponseEntity.noContent().build()
        }
    }

    @PostMapping("/farms/{farmId}/weather/fetch")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Fetch weather data from external API for a farm")
    fun fetchWeatherDataForFarm(@PathVariable farmId: Long): ResponseEntity<List<WeatherDataResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val weatherDataList = weatherService.fetchWeatherDataForFarm(userId, farmId)
        return ResponseEntity.ok(weatherDataList)
    }

    @GetMapping("/farms/{farmId}/weather/forecast")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get weather forecast for a farm")
    fun getWeatherForecast(
        @PathVariable farmId: Long,
        @RequestParam(defaultValue = "7") days: Int
    ): ResponseEntity<WeatherForecastResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val forecast = weatherService.getWeatherForecast(userId, farmId, days)
        return ResponseEntity.ok(forecast)
    }

    @GetMapping("/farms/{farmId}/weather/avg-rainfall")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Calculate average rainfall for a specific date range")
    fun calculateAverageRainfall(
        @PathVariable farmId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<Map<String, BigDecimal>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val avgRainfall = weatherService.calculateAverageRainfall(userId, farmId, startDate, endDate)
        return if (avgRainfall != null) {
            ResponseEntity.ok(mapOf("averageRainfall" to avgRainfall))
        } else {
            ResponseEntity.noContent().build()
        }
    }

    @GetMapping("/farms/{farmId}/weather/avg-temperature")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Calculate average temperature for a specific date range")
    fun calculateAverageTemperature(
        @PathVariable farmId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<Map<String, BigDecimal>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val avgTemperature = weatherService.calculateAverageTemperature(userId, farmId, startDate, endDate)
        return if (avgTemperature != null) {
            ResponseEntity.ok(mapOf("averageTemperature" to avgTemperature))
        } else {
            ResponseEntity.noContent().build()
        }
    }
}