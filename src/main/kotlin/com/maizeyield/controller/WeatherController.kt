package com.maizeyield.controller

import com.maizeyield.dto.WeatherDataCreateRequest
import com.maizeyield.dto.WeatherDataResponse
import com.maizeyield.dto.WeatherDataUpdateRequest
import com.maizeyield.dto.WeatherForecastResponse
import com.maizeyield.service.AuthService
import com.maizeyield.service.WeatherService
import com.maizeyield.service.external.WeatherAlert
import com.maizeyield.service.impl.WeatherServiceImpl
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
@Tag(name = "Weather", description = "Weather data operations with external API integration")
class WeatherController(
    private val weatherService: WeatherService,
    private val weatherServiceImpl: WeatherServiceImpl, // For additional methods
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
    @Operation(summary = "Add weather data manually")
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
    @Operation(summary = "Fetch current weather data from external API")
    fun fetchWeatherDataForFarm(@PathVariable farmId: Long): ResponseEntity<List<WeatherDataResponse>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val weatherDataList = weatherService.fetchWeatherDataForFarm(userId, farmId)
        return ResponseEntity.ok(weatherDataList)
    }

    @GetMapping("/farms/{farmId}/weather/forecast")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get weather forecast from external API")
    fun getWeatherForecast(
        @PathVariable farmId: Long,
        @RequestParam(defaultValue = "7") days: Int
    ): ResponseEntity<WeatherForecastResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val forecast = weatherService.getWeatherForecast(userId, farmId, days)
        return ResponseEntity.ok(forecast)
    }

    @GetMapping("/farms/{farmId}/weather/historical/{date}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get historical weather data for a specific date")
    fun getHistoricalWeatherData(
        @PathVariable farmId: Long,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<WeatherDataResponse> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val weatherData = weatherServiceImpl.fetchHistoricalWeatherData(userId, farmId, date)
        return if (weatherData != null) {
            ResponseEntity.ok(weatherData)
        } else {
            ResponseEntity.noContent().build()
        }
    }

    @GetMapping("/farms/{farmId}/weather/alerts")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get weather alerts for a farm")
    fun getWeatherAlerts(@PathVariable farmId: Long): ResponseEntity<List<WeatherAlert>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val alerts = weatherServiceImpl.getWeatherAlerts(userId, farmId)
        return ResponseEntity.ok(alerts)
    }

    @GetMapping("/farms/{farmId}/weather/statistics")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get weather statistics for a date range")
    fun getWeatherStatistics(
        @PathVariable farmId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<Map<String, Any>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val statistics = weatherServiceImpl.getWeatherStatistics(userId, farmId, startDate, endDate)
        return ResponseEntity.ok(statistics)
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

    @PostMapping("/farms/{farmId}/weather/batch-fetch")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Batch fetch historical weather data for multiple dates")
    fun batchFetchHistoricalData(
        @PathVariable farmId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<Map<String, Any>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())

        val results = mutableMapOf<String, Any>()
        var successCount = 0
        var errorCount = 0
        val errors = mutableListOf<String>()

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            try {
                val weatherData = weatherServiceImpl.fetchHistoricalWeatherData(userId, farmId, currentDate)
                if (weatherData != null) {
                    successCount++
                } else {
                    errorCount++
                    errors.add("No data available for $currentDate")
                }
            } catch (e: Exception) {
                errorCount++
                errors.add("Failed to fetch data for $currentDate: ${e.message}")
            }
            currentDate = currentDate.plusDays(1)
        }

        results["successCount"] = successCount
        results["errorCount"] = errorCount
        results["errors"] = errors.take(10) // Limit error messages
        results["totalDays"] = endDate.toEpochDay() - startDate.toEpochDay() + 1

        return ResponseEntity.ok(results)
    }

    @GetMapping("/farms/{farmId}/weather/data-quality")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get weather data quality metrics")
    fun getWeatherDataQuality(
        @PathVariable farmId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<Map<String, Any?>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())
        val statistics = weatherServiceImpl.getWeatherStatistics(userId, farmId, startDate, endDate)

        // Extract data quality information
        val dataQuality = statistics["dataQuality"] as? Map<String, Any> ?: emptyMap()

        return ResponseEntity.ok(
                mapOf(
                "period" to mapOf(
                    "startDate" to startDate,
                    "endDate" to endDate
                ),
                "quality" to dataQuality,
                "recordCount" to statistics["recordCount"],
                "recommendations" to generateDataQualityRecommendations(dataQuality)
            )
        )
    }

    @PostMapping("/farms/{farmId}/weather/refresh-cache")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Refresh weather data cache for a farm")
    fun refreshWeatherCache(@PathVariable farmId: Long): ResponseEntity<Map<String, String>> {
        val userId = authService.getUserIdFromToken(SecurityContextUtil.getTokenFromRequest())

        try {
            // Fetch latest weather data to refresh cache
            weatherService.fetchWeatherDataForFarm(userId, farmId)
            return ResponseEntity.ok(mapOf("message" to "Weather cache refreshed successfully"))
        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to refresh weather cache: ${e.message}"))
        }
    }

    private fun generateDataQualityRecommendations(dataQuality: Map<String, Any>): List<String> {
        val recommendations = mutableListOf<String>()

        val completeness = dataQuality["completeness"] as? Double ?: 0.0
        val tempQuality = dataQuality["temperatureDataQuality"] as? Double ?: 0.0
        val rainfallQuality = dataQuality["rainfallDataQuality"] as? Double ?: 0.0

        if (completeness < 80.0) {
            recommendations.add("Data completeness is low (${String.format("%.1f", completeness)}%). Consider fetching missing historical data.")
        }

        if (tempQuality < 90.0) {
            recommendations.add("Temperature data quality could be improved (${String.format("%.1f", tempQuality)}%). Check sensor calibration.")
        }

        if (rainfallQuality < 90.0) {
            recommendations.add("Rainfall data quality needs attention (${String.format("%.1f", rainfallQuality)}%). Verify rain gauge functionality.")
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Weather data quality is excellent. No action required.")
        }

        return recommendations
    }
}