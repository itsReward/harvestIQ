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
import com.maizeyield.service.external.WeatherAlert
import com.maizeyield.service.external.WeatherApiService
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class WeatherServiceImpl(
    private val weatherDataRepository: WeatherDataRepository,
    private val farmRepository: FarmRepository,
    private val farmService: FarmService,
    private val weatherApiService: WeatherApiService // Inject the new external service
) : WeatherService {

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
        if (farm.latitude == null || farm.longitude == null) {
            throw IllegalArgumentException("Farm does not have latitude/longitude coordinates")
        }

        try {
            logger.info { "Fetching current weather data for farm: ${farm.name}" }

            // Use the new external weather service
            val currentWeatherResponse = weatherApiService.fetchCurrentWeather(farm)

            return if (currentWeatherResponse != null) {
                // Save current weather data to database
                val existingData = weatherDataRepository.findByFarmAndDate(farm, currentWeatherResponse.date)

                if (existingData.isEmpty) {
                    val weatherData = WeatherData(
                        farm = farm,
                        date = currentWeatherResponse.date,
                        minTemperature = currentWeatherResponse.minTemperature,
                        maxTemperature = currentWeatherResponse.maxTemperature,
                        averageTemperature = currentWeatherResponse.averageTemperature,
                        rainfallMm = currentWeatherResponse.rainfallMm,
                        humidityPercentage = currentWeatherResponse.humidityPercentage,
                        windSpeedKmh = currentWeatherResponse.windSpeedKmh,
                        solarRadiation = currentWeatherResponse.solarRadiation,
                        source = currentWeatherResponse.source
                    )

                    val savedWeatherData = weatherDataRepository.save(weatherData)
                    listOf(mapToWeatherDataResponse(savedWeatherData))
                } else {
                    listOf(currentWeatherResponse)
                }
            } else {
                emptyList()
            }

        } catch (e: Exception) {
            logger.error(e) { "Error fetching weather data for farm: ${farm.name}" }
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
        if (farm.latitude == null || farm.longitude == null) {
            throw IllegalArgumentException("Farm does not have latitude/longitude coordinates")
        }

        try {
            logger.info { "Fetching weather forecast for farm: ${farm.name}" }

            // Use the new external weather service
            val forecastData = weatherApiService.fetchWeatherForecast(farm, days)

            return WeatherForecastResponse(forecastData)

        } catch (e: Exception) {
            logger.error(e) { "Error fetching weather forecast for farm: ${farm.name}" }
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

    /**
     * Fetch weather alerts for a farm
     */
    fun getWeatherAlerts(userId: Long, farmId: Long): List<WeatherAlert> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return try {
            weatherApiService.fetchWeatherAlerts(farm)
        } catch (e: Exception) {
            logger.error(e) { "Error fetching weather alerts for farm: ${farm.name}" }
            emptyList()
        }
    }

    /**
     * Fetch historical weather data for a specific date
     */
    fun fetchHistoricalWeatherData(userId: Long, farmId: Long, date: LocalDate): WeatherDataResponse? {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return try {
            weatherApiService.fetchHistoricalWeather(farm, date)
        } catch (e: Exception) {
            logger.error(e) { "Error fetching historical weather data for farm: ${farm.name} on date: $date" }
            null
        }
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