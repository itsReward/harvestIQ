package com.maizeyield.service.impl

import com.maizeyield.dto.WeatherDataCreateRequest
import com.maizeyield.dto.WeatherDataResponse
import com.maizeyield.dto.WeatherDataUpdateRequest
import com.maizeyield.dto.WeatherForecastResponse
import com.maizeyield.exception.ExternalServiceException
import com.maizeyield.exception.ResourceNotFoundException
import com.maizeyield.exception.UnauthorizedAccessException
import com.maizeyield.model.WeatherData
import com.maizeyield.repository.FarmRepository
import com.maizeyield.repository.WeatherDataRepository
import com.maizeyield.service.FarmService
import com.maizeyield.service.WeatherService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class WeatherServiceImpl(
    private val weatherDataRepository: WeatherDataRepository,
    private val farmRepository: FarmRepository,
    private val farmService: FarmService,
    private val restTemplate: RestTemplate
) : WeatherService {

    @Value("\${weather.api.key:dummy-key}")
    private lateinit var apiKey: String

    @Value("\${weather.api.url:https://api.weatherapi.com/v1}")
    private lateinit var apiUrl: String

    override fun getWeatherDataByFarmId(userId: Long, farmId: Long): List<WeatherDataResponse> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return weatherDataRepository.findByFarm(farm).map { weatherData ->
            mapToWeatherDataResponse(weatherData)
        }
    }

    override fun getWeatherDataByDateRange(
        userId: Long,
        farmId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<WeatherDataResponse> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return weatherDataRepository.findByFarmAndDateBetween(farm, startDate, endDate).map { weatherData ->
            mapToWeatherDataResponse(weatherData)
        }
    }

    override fun getWeatherDataById(userId: Long, weatherDataId: Long): WeatherDataResponse {
        val weatherData = weatherDataRepository.findById(weatherDataId)
            .orElseThrow { ResourceNotFoundException("Weather data not found with ID: $weatherDataId") }

        if (!farmService.isFarmOwner(userId, weatherData.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to access this weather data")
        }

        return mapToWeatherDataResponse(weatherData)
    }

    @Transactional
    override fun addWeatherData(userId: Long, request: WeatherDataCreateRequest): WeatherDataResponse {
        if (!farmService.isFarmOwner(userId, request.farmId)) {
            throw UnauthorizedAccessException("You don't have permission to add weather data to this farm")
        }

        val farm = farmRepository.findById(request.farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: ${request.farmId}") }

        // Check if weather data already exists for this date
        weatherDataRepository.findByFarmAndDate(farm, request.date).ifPresent {
            throw IllegalArgumentException("Weather data already exists for this farm and date")
        }

        val weatherData = WeatherData(
            farm = farm,
            date = request.date,
            minTemperature = request.minTemperature,
            maxTemperature = request.maxTemperature,
            averageTemperature = request.averageTemperature,
            rainfallMm = request.rainfallMm,
            humidityPercentage = request.humidityPercentage,
            windSpeedKmh = request.windSpeedKmh,
            solarRadiation = request.solarRadiation,
            source = request.source
        )

        val savedWeatherData = weatherDataRepository.save(weatherData)

        return mapToWeatherDataResponse(savedWeatherData)
    }

    @Transactional
    override fun updateWeatherData(
        userId: Long,
        weatherDataId: Long,
        request: WeatherDataUpdateRequest
    ): WeatherDataResponse {
        val weatherData = weatherDataRepository.findById(weatherDataId)
            .orElseThrow { ResourceNotFoundException("Weather data not found with ID: $weatherDataId") }

        if (!farmService.isFarmOwner(userId, weatherData.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to update this weather data")
        }

        val updatedWeatherData = weatherData.copy(
            minTemperature = request.minTemperature ?: weatherData.minTemperature,
            maxTemperature = request.maxTemperature ?: weatherData.maxTemperature,
            averageTemperature = request.averageTemperature ?: weatherData.averageTemperature,
            rainfallMm = request.rainfallMm ?: weatherData.rainfallMm,
            humidityPercentage = request.humidityPercentage ?: weatherData.humidityPercentage,
            windSpeedKmh = request.windSpeedKmh ?: weatherData.windSpeedKmh,
            solarRadiation = request.solarRadiation ?: weatherData.solarRadiation,
            source = request.source ?: weatherData.source
        )

        val savedWeatherData = weatherDataRepository.save(updatedWeatherData)

        return mapToWeatherDataResponse(savedWeatherData)
    }

    @Transactional
    override fun deleteWeatherData(userId: Long, weatherDataId: Long): Boolean {
        val weatherData = weatherDataRepository.findById(weatherDataId)
            .orElseThrow { ResourceNotFoundException("Weather data not found with ID: $weatherDataId") }

        if (!farmService.isFarmOwner(userId, weatherData.farm.id!!)) {
            throw UnauthorizedAccessException("You don't have permission to delete this weather data")
        }

        weatherDataRepository.delete(weatherData)
        return true
    }

    override fun getLatestWeatherData(userId: Long, farmId: Long): WeatherDataResponse? {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return weatherDataRepository.findLatestByFarm(farm)
            .map { mapToWeatherDataResponse(it) }
            .orElse(null)
    }

    @Transactional
    override fun fetchWeatherDataForFarm(userId: Long, farmId: Long): List<WeatherDataResponse> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        // Check if farm has coordinates
        val latitude = farm.latitude ?: throw IllegalArgumentException("Farm does not have latitude coordinates")
        val longitude = farm.longitude ?: throw IllegalArgumentException("Farm does not have longitude coordinates")

        try {
            // Build the API URL for historical weather data (last 7 days)
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(7)

            val url = UriComponentsBuilder.fromHttpUrl("$apiUrl/history.json")
                .queryParam("key", apiKey)
                .queryParam("q", "$latitude,$longitude")
                .queryParam("dt", startDate.toString())
                .queryParam("end_dt", endDate.toString())
                .toUriString()

            // Make the API call
            val response = restTemplate.getForEntity(url, Map::class.java)

            if (response.statusCode == HttpStatus.OK && response.body != null) {
                val responseBody = response.body as Map<*, *>
                val forecast = responseBody["forecast"] as? Map<*, *>
                val forecastDays = forecast?.get("forecastday") as? List<*>

                if (forecastDays != null) {
                    val weatherDataList = mutableListOf<WeatherData>()

                    for (day in forecastDays) {
                        day as Map<*, *>
                        val date = LocalDate.parse(day["date"] as String)
                        val dayData = day["day"] as Map<*, *>

                        // Check if we already have data for this date
                        if (weatherDataRepository.findByFarmAndDate(farm, date).isPresent) {
                            continue
                        }

                        val weatherData = WeatherData(
                            farm = farm,
                            date = date,
                            minTemperature = BigDecimal(dayData["mintemp_c"].toString()),
                            maxTemperature = BigDecimal(dayData["maxtemp_c"].toString()),
                            averageTemperature = BigDecimal(dayData["avgtemp_c"].toString()),
                            rainfallMm = BigDecimal(dayData["totalprecip_mm"].toString()),
                            humidityPercentage = BigDecimal(dayData["avghumidity"].toString()),
                            windSpeedKmh = BigDecimal(dayData["maxwind_kph"].toString()),
                            source = "WeatherAPI"
                        )

                        weatherDataList.add(weatherData)
                    }

                    // Save all weather data
                    if (weatherDataList.isNotEmpty()) {
                        val savedWeatherData = weatherDataRepository.saveAll(weatherDataList)
                        return savedWeatherData.map { mapToWeatherDataResponse(it) }
                    }
                }
            }

            return emptyList()
        } catch (e: Exception) {
            logger.error(e) { "Error fetching weather data from external API" }
            throw ExternalServiceException("Failed to fetch weather data from external API", "WeatherAPI")
        }
    }

    override fun getWeatherForecast(userId: Long, farmId: Long, days: Int): WeatherForecastResponse {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        // Check if farm has coordinates
        val latitude = farm.latitude ?: throw IllegalArgumentException("Farm does not have latitude coordinates")
        val longitude = farm.longitude ?: throw IllegalArgumentException("Farm does not have longitude coordinates")

        try {
            // Limit days to 10 (or whatever the API supports)
            val forecastDays = if (days > 10) 10 else days

            // Build the API URL for forecast
            val url = UriComponentsBuilder.fromHttpUrl("$apiUrl/forecast.json")
                .queryParam("key", apiKey)
                .queryParam("q", "$latitude,$longitude")
                .queryParam("days", forecastDays)
                .toUriString()

            // Make the API call
            val response = restTemplate.getForEntity(url, Map::class.java)

            if (response.statusCode == HttpStatus.OK && response.body != null) {
                val responseBody = response.body as Map<*, *>
                val forecast = responseBody["forecast"] as? Map<*, *>
                val forecastDays = forecast?.get("forecastday") as? List<*>

                if (forecastDays != null) {
                    val forecasts = mutableListOf<WeatherDataResponse>()

                    for (day in forecastDays) {
                        day as Map<*, *>
                        val date = LocalDate.parse(day["date"] as String)
                        val dayData = day["day"] as Map<*, *>

                        val weatherData = WeatherDataResponse(
                            id = -1, // Not saved to database
                            farmId = farm.id!!,
                            date = date,
                            minTemperature = BigDecimal(dayData["mintemp_c"].toString()),
                            maxTemperature = BigDecimal(dayData["maxtemp_c"].toString()),
                            averageTemperature = BigDecimal(dayData["avgtemp_c"].toString()),
                            rainfallMm = BigDecimal(dayData["totalprecip_mm"].toString()),
                            humidityPercentage = BigDecimal(dayData["avghumidity"].toString()),
                            windSpeedKmh = BigDecimal(dayData["maxwind_kph"].toString()),
                            source = "WeatherAPI Forecast"
                        )

                        forecasts.add(weatherData)
                    }

                    return WeatherForecastResponse(forecasts)
                }
            }

            throw ExternalServiceException("Failed to parse weather forecast from API", "WeatherAPI")
        } catch (e: Exception) {
            logger.error(e) { "Error fetching weather forecast from external API" }
            throw ExternalServiceException("Failed to fetch weather forecast from external API", "WeatherAPI")
        }
    }

    override fun calculateAverageRainfall(
        userId: Long,
        farmId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): BigDecimal? {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        val avgRainfall = weatherDataRepository.findAverageRainfallByFarmAndDateRange(farm, startDate, endDate)

        return avgRainfall?.let { BigDecimal(it).setScale(2, RoundingMode.HALF_UP) }
    }

    override fun calculateAverageTemperature(
        userId: Long,
        farmId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): BigDecimal? {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        val avgTemperature = weatherDataRepository.findAverageTemperatureByFarmAndDateRange(farm, startDate, endDate)

        return avgTemperature?.let { BigDecimal(it).setScale(2, RoundingMode.HALF_UP) }
    }

    // Helper method to map WeatherData entity to WeatherDataResponse DTO
    private fun mapToWeatherDataResponse(weatherData: WeatherData): WeatherDataResponse {
        return WeatherDataResponse(
            id = weatherData.id!!,
            farmId = weatherData.farm.id!!,
            date = weatherData.date,
            minTemperature = weatherData.minTemperature,
            maxTemperature = weatherData.maxTemperature,
            averageTemperature = weatherData.averageTemperature,
            rainfallMm = weatherData.rainfallMm,
            humidityPercentage = weatherData.humidityPercentage,
            windSpeedKmh = weatherData.windSpeedKmh,
            solarRadiation = weatherData.solarRadiation,
            source = weatherData.source
        )
    }
}