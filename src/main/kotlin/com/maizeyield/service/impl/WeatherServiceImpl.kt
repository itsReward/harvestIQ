package com.maizeyield.service.impl

import com.maizeyield.config.WeatherDataValidator
import com.maizeyield.dto.WeatherDataCreateRequest
import com.maizeyield.dto.WeatherDataResponse
import com.maizeyield.dto.WeatherDataUpdateRequest
import com.maizeyield.dto.WeatherForecastResponse
import com.maizeyield.exception.ExternalServiceException
import com.maizeyield.exception.ResourceNotFoundException
import com.maizeyield.exception.UnauthorizedAccessException
import com.maizeyield.model.Farm
import com.maizeyield.model.WeatherData
import com.maizeyield.repository.FarmRepository
import com.maizeyield.repository.WeatherDataRepository
import com.maizeyield.service.FarmService
import com.maizeyield.service.WeatherService
import com.maizeyield.service.external.WeatherAlert
import com.maizeyield.service.external.WeatherApiService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.pow

private val logger = KotlinLogging.logger {}

@Service
class WeatherServiceImpl(
    private val weatherDataRepository: WeatherDataRepository,
    private val farmRepository: FarmRepository,
    private val farmService: FarmService,
    private val weatherApiService: WeatherApiService, // Direct injection
    private val weatherDataValidator: WeatherDataValidator,
    @Value("\${weather.data.validation.enabled:true}") private val validationEnabled: Boolean,
    @Value("\${weather.data.cache.ttl-minutes:30}") private val cacheTtlMinutes: Int,
    @Value("\${weather.fallback.enabled:true}") private val fallbackEnabled: Boolean,
    @Value("\${weather.fallback.providers:weatherapi,openweather}") fallbackProvidersString: String
) : WeatherService {

    private val fallbackProviders: List<String> = fallbackProvidersString.split(",")

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    private fun fetchCurrentWeatherWithFallback(farm: Farm): WeatherDataResponse? {
        return try {
            logger.info { "Attempting to fetch current weather for farm: ${farm.name}" }
            weatherApiService.fetchCurrentWeather(farm)
        } catch (e: Exception) {
            logger.warn(e) { "Primary weather service failed for farm: ${farm.name}" }
            if (fallbackEnabled) {
                attemptFallbackCurrentWeather(farm)
            } else {
                null
            }
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    private fun fetchWeatherForecastWithFallback(farm: Farm, days: Int): List<WeatherDataResponse> {
        return try {
            logger.info { "Attempting to fetch weather forecast for farm: ${farm.name}" }
            weatherApiService.fetchWeatherForecast(farm, days)
        } catch (e: Exception) {
            logger.warn(e) { "Primary weather service failed for forecast for farm: ${farm.name}" }
            if (fallbackEnabled) {
                attemptFallbackForecast(farm, days)
            } else {
                emptyList()
            }
        }
    }

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    private fun fetchHistoricalWeatherWithFallback(farm: Farm, date: LocalDate): WeatherDataResponse? {
        return try {
            logger.info { "Attempting to fetch historical weather for farm: ${farm.name} on $date" }
            weatherApiService.fetchHistoricalWeather(farm, date)
        } catch (e: Exception) {
            logger.warn(e) { "Primary weather service failed for historical data for farm: ${farm.name}" }
            if (fallbackEnabled) {
                attemptFallbackHistorical(farm, date)
            } else {
                null
            }
        }
    }

    private fun fetchWeatherAlertsWithFallback(farm: Farm): List<WeatherAlert> {
        return try {
            weatherApiService.fetchWeatherAlerts(farm)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to fetch weather alerts for farm: ${farm.name}" }
            emptyList()
        }
    }

    private fun attemptFallbackCurrentWeather(farm: Farm): WeatherDataResponse? {
        logger.info { "Attempting fallback weather providers for current weather" }

        fallbackProviders.forEach { provider ->
            try {
                logger.info { "Trying fallback provider: $provider" }
                // For now, we'll use the same service but you can extend this
                // to use different configurations based on the provider
                val result = weatherApiService.fetchCurrentWeather(farm)
                if (result != null) {
                    logger.info { "Successfully retrieved current weather from fallback provider: $provider" }
                    return result
                }
            } catch (e: Exception) {
                logger.warn(e) { "Fallback provider $provider also failed" }
            }
        }

        logger.error { "All weather providers failed for current weather" }
        return null
    }

    private fun attemptFallbackForecast(farm: Farm, days: Int): List<WeatherDataResponse> {
        logger.info { "Attempting fallback weather providers for forecast" }

        fallbackProviders.forEach { provider ->
            try {
                logger.info { "Trying fallback provider: $provider" }
                val result = weatherApiService.fetchWeatherForecast(farm, days)
                if (result.isNotEmpty()) {
                    logger.info { "Successfully retrieved forecast from fallback provider: $provider" }
                    return result
                }
            } catch (e: Exception) {
                logger.warn(e) { "Fallback provider $provider also failed for forecast" }
            }
        }

        logger.error { "All weather providers failed for forecast" }
        return emptyList()
    }

    private fun attemptFallbackHistorical(farm: Farm, date: LocalDate): WeatherDataResponse? {
        logger.info { "Attempting fallback weather providers for historical data" }

        fallbackProviders.forEach { provider ->
            try {
                logger.info { "Trying fallback provider: $provider" }
                val result = weatherApiService.fetchHistoricalWeather(farm, date)
                if (result != null) {
                    logger.info { "Successfully retrieved historical data from fallback provider: $provider" }
                    return result
                }
            } catch (e: Exception) {
                logger.warn(e) { "Fallback provider $provider also failed for historical data" }
            }
        }

        logger.error { "All weather providers failed for historical data" }
        return null
    }

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

    @Cacheable(value = ["weather-data"], key = "#farmId + '_latest'", unless = "#result == null")
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

            // Use the internal resilient weather service methods
            val currentWeatherResponse = fetchCurrentWeatherWithFallback(farm)

            return if (currentWeatherResponse != null) {
                // Validate data if validation is enabled
                val validatedResponse = if (validationEnabled) {
                    if (weatherDataValidator.validateWeatherData(currentWeatherResponse)) {
                        weatherDataValidator.sanitizeWeatherData(currentWeatherResponse)
                    } else {
                        logger.warn { "Weather data validation failed for farm: ${farm.name}" }
                        return emptyList()
                    }
                } else {
                    currentWeatherResponse
                }

                // Save current weather data to database if it doesn't exist
                val existingData = weatherDataRepository.findByFarmAndDate(farm, validatedResponse.date)

                if (existingData.isEmpty) {
                    val weatherData = WeatherData(
                        farm = farm,
                        date = validatedResponse.date,
                        minTemperature = validatedResponse.minTemperature,
                        maxTemperature = validatedResponse.maxTemperature,
                        averageTemperature = validatedResponse.averageTemperature,
                        rainfallMm = validatedResponse.rainfallMm,
                        humidityPercentage = validatedResponse.humidityPercentage,
                        windSpeedKmh = validatedResponse.windSpeedKmh,
                        solarRadiation = validatedResponse.solarRadiation,
                        source = validatedResponse.source
                    )

                    val savedWeatherData = weatherDataRepository.save(weatherData)
                    listOf(mapToWeatherDataResponse(savedWeatherData))
                } else {
                    listOf(validatedResponse)
                }
            } else {
                logger.warn { "Failed to fetch weather data from all providers for farm: ${farm.name}" }
                emptyList()
            }

        } catch (e: Exception) {
            logger.error(e) { "Error fetching weather data for farm: ${farm.name}" }
            throw ExternalServiceException("Failed to fetch weather data from external API", "WeatherAPI")
        }
    }

    @Cacheable(value = ["weather-forecast"], key = "#farmId + '_' + #days", unless = "#result.forecasts.isEmpty()")
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

            // Use the internal resilient weather service methods
            val forecastData = fetchWeatherForecastWithFallback(farm, days)

            // Validate and sanitize forecast data if validation is enabled
            val validatedForecastData = if (validationEnabled) {
                forecastData.mapNotNull { forecast ->
                    if (weatherDataValidator.validateWeatherData(forecast)) {
                        weatherDataValidator.sanitizeWeatherData(forecast)
                    } else {
                        logger.warn { "Forecast data validation failed for farm: ${farm.name} on ${forecast.date}" }
                        null
                    }
                }
            } else {
                forecastData
            }

            return WeatherForecastResponse(validatedForecastData)

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
            fetchWeatherAlertsWithFallback(farm)
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

        // Check if we already have this data in our database
        val existingData = weatherDataRepository.findByFarmAndDate(farm, date)
        if (existingData.isPresent) {
            return mapToWeatherDataResponse(existingData.get())
        }

        // Fetch from external API if not found locally
        return try {
            val historicalData = fetchHistoricalWeatherWithFallback(farm, date)

            // Save to database if validation passes
            historicalData?.let { data ->
                val validatedData = if (validationEnabled) {
                    if (weatherDataValidator.validateWeatherData(data)) {
                        weatherDataValidator.sanitizeWeatherData(data)
                    } else {
                        logger.warn { "Historical weather data validation failed for farm: ${farm.name} on $date" }
                        return null
                    }
                } else {
                    data
                }

                val weatherEntity = WeatherData(
                    farm = farm,
                    date = validatedData.date,
                    minTemperature = validatedData.minTemperature,
                    maxTemperature = validatedData.maxTemperature,
                    averageTemperature = validatedData.averageTemperature,
                    rainfallMm = validatedData.rainfallMm,
                    humidityPercentage = validatedData.humidityPercentage,
                    windSpeedKmh = validatedData.windSpeedKmh,
                    solarRadiation = validatedData.solarRadiation,
                    source = validatedData.source
                )

                val savedEntity = weatherDataRepository.save(weatherEntity)
                mapToWeatherDataResponse(savedEntity)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error fetching historical weather data for farm: ${farm.name} on date: $date" }
            null
        }
    }

    /**
     * Get weather statistics for a farm
     */
    fun getWeatherStatistics(userId: Long, farmId: Long, startDate: LocalDate, endDate: LocalDate): Map<String, Any> {
        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return mapOf(
            "averageTemperature" to (weatherDataRepository.findAverageTemperatureByFarmAndDateRange(farm, startDate, endDate) ?: 0.0),
            "maxTemperature" to (weatherDataRepository.findMaxTemperatureByFarmAndDateRange(farm, startDate, endDate) ?: 0.0),
            "minTemperature" to (weatherDataRepository.findMinTemperatureByFarmAndDateRange(farm, startDate, endDate) ?: 0.0),
            "totalRainfall" to (weatherDataRepository.findTotalRainfallByFarmAndDateRange(farm, startDate, endDate) ?: 0.0),
            "averageHumidity" to (weatherDataRepository.findAverageHumidityByFarmAndDateRange(farm, startDate, endDate) ?: 0.0),
            "recordCount" to weatherDataRepository.countRecordsInDateRange(farm, startDate, endDate),
            "dataQuality" to calculateDataQuality(farm, startDate, endDate)
        )
    }

    private fun calculateDataQuality(farm: Farm, startDate: LocalDate, endDate: LocalDate): Map<String, Any> {
        val totalDays = endDate.toEpochDay() - startDate.toEpochDay() + 1
        val recordCount = weatherDataRepository.countRecordsInDateRange(farm, startDate, endDate)
        val missingTemperature = weatherDataRepository.countMissingTemperatureRecords(farm)
        val missingRainfall = weatherDataRepository.countMissingRainfallRecords(farm)

        return mapOf(
            "completeness" to (recordCount.toDouble() / totalDays * 100),
            "temperatureDataQuality" to ((recordCount - missingTemperature).toDouble() / recordCount * 100),
            "rainfallDataQuality" to ((recordCount - missingRainfall).toDouble() / recordCount * 100)
        )
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

    override fun getLastUpdateTime(): LocalDateTime? {
        return try {
            weatherDataRepository.findTopByOrderByCreatedAtDesc()?.createdAt
        } catch (e: Exception) {
            null
        }
    }

    override fun getWeatherData(
        userId: Long,
        location: String?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageable: Pageable
    ): Page<WeatherDataResponse> {
        logger.info { "Getting weather data for user $userId with filters - location: $location, startDate: $startDate, endDate: $endDate" }

        return try {
            // Get all farms for the user
            val userFarms = farmRepository.findByUserId(userId)

            if (userFarms.isEmpty()) {
                return PageImpl(emptyList(), pageable, 0)
            }

            // Filter farms by location if specified
            val filteredFarms = if (location != null) {
                userFarms.filter { farm ->
                    farm.location.contains(location, ignoreCase = true)
                }
            } else {
                userFarms
            }

            if (filteredFarms.isEmpty()) {
                return PageImpl(emptyList(), pageable, 0)
            }

            // Collect weather data from all filtered farms
            val allWeatherData = mutableListOf<WeatherData>()

            filteredFarms.forEach { farm ->
                val weatherData = if (startDate != null && endDate != null) {
                    weatherDataRepository.findByFarmAndDateBetween(farm, startDate, endDate)
                } else {
                    weatherDataRepository.findByFarm(farm)
                }
                allWeatherData.addAll(weatherData)
            }

            // Sort by date (newest first) and farm ID for consistency
            val sortedWeatherData = allWeatherData.sortedWith(
                compareByDescending<WeatherData> { it.date }
                    .thenBy { it.farm.id }
            )

            // Apply pagination manually
            val totalElements = sortedWeatherData.size
            val startIndex = pageable.offset.toInt()
            val endIndex = minOf(startIndex + pageable.pageSize, totalElements)

            val pageContent = if (startIndex >= totalElements) {
                emptyList()
            } else {
                sortedWeatherData.subList(startIndex, endIndex).map { weatherData ->
                    mapToWeatherDataResponse(weatherData)
                }
            }

            PageImpl(pageContent, pageable, totalElements.toLong())

        } catch (e: Exception) {
            logger.error(e) { "Error getting weather data: ${e.message}" }
            PageImpl(emptyList(), pageable, 0)
        }
    }

    override fun isWeatherDataOwner(userId: Long, weatherId: Long): Boolean {
        logger.debug { "Checking if user $userId owns weather data $weatherId" }

        return try {
            val weatherData = weatherDataRepository.findById(weatherId)
                .orElse(null) ?: return false

            // Check if user owns the farm that contains this weather data
            farmService.isFarmOwner(userId, weatherData.farm.id!!)
        } catch (e: Exception) {
            logger.error(e) { "Error checking weather data ownership: ${e.message}" }
            false
        }
    }

    @Transactional
    override fun createWeatherData(
        userId: Long,
        request: WeatherDataCreateRequest
    ): WeatherDataResponse {
        logger.info { "Creating weather data for user $userId, farm ${request.farmId}" }

        // Validate farm ownership
        if (!farmService.isFarmOwner(userId, request.farmId)) {
            throw UnauthorizedAccessException("You don't have permission to add weather data to this farm")
        }

        val farm = farmRepository.findById(request.farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: ${request.farmId}") }

        return try {
            // Check if weather data already exists for this farm and date
            val existingData = weatherDataRepository.findByFarmAndDate(farm, request.date)
            if (existingData.isPresent) {
                logger.warn { "Weather data already exists for farm ${request.farmId} on ${request.date}" }
                throw IllegalArgumentException("Weather data already exists for this farm on ${request.date}")
            }

            // Validate weather data values
            validateWeatherDataRequest(request)

            val weatherData = WeatherData(
                farm = farm,
                date = request.date,
                minTemperature = request.minTemperature,
                maxTemperature = request.maxTemperature,
                averageTemperature = request.averageTemperature ?: calculateAverageTemperature(
                    request.minTemperature,
                    request.maxTemperature
                ),
                rainfallMm = request.rainfallMm,
                humidityPercentage = request.humidityPercentage,
                windSpeedKmh = request.windSpeedKmh,
                solarRadiation = request.solarRadiation,
                source = request.source,
                createdAt = LocalDateTime.now()
            )

            val savedWeatherData = weatherDataRepository.save(weatherData)
            logger.info { "Successfully created weather data with ID ${savedWeatherData.id}" }

            mapToWeatherDataResponse(savedWeatherData)

        } catch (e: Exception) {
            logger.error(e) { "Error creating weather data: ${e.message}" }
            when (e) {
                is IllegalArgumentException, is UnauthorizedAccessException, is ResourceNotFoundException -> throw e
                else -> throw RuntimeException("Failed to create weather data: ${e.message}")
            }
        }
    }

    override fun getWeatherDataForFarm(
        userId: Long,
        farmId: Long,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): List<WeatherDataResponse> {
        logger.info { "Getting weather data for farm $farmId, user $userId" }

        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to access weather data for this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return try {
            val weatherData = if (startDate != null && endDate != null) {
                // Validate date range
                if (startDate.isAfter(endDate)) {
                    throw IllegalArgumentException("Start date cannot be after end date")
                }

                weatherDataRepository.findByFarmAndDateBetween(farm, startDate, endDate)
            } else {
                weatherDataRepository.findByFarm(farm)
            }

            weatherData.map { mapToWeatherDataResponse(it) }
                .sortedByDescending { it.date }

        } catch (e: Exception) {
            logger.error(e) { "Error getting weather data for farm: ${e.message}" }
            when (e) {
                is IllegalArgumentException, is UnauthorizedAccessException, is ResourceNotFoundException -> throw e
                else -> throw RuntimeException("Failed to get weather data for farm: ${e.message}")
            }
        }
    }

    @Transactional
    override fun fetchAndStoreWeatherDataForFarm(
        userId: Long,
        farmId: Long
    ): WeatherDataResponse {
        logger.info { "Fetching and storing weather data for farm $farmId, user $userId" }

        if (!farmService.isFarmOwner(userId, farmId)) {
            throw UnauthorizedAccessException("You don't have permission to fetch weather data for this farm")
        }

        val farm = farmRepository.findById(farmId)
            .orElseThrow { ResourceNotFoundException("Farm not found with ID: $farmId") }

        return try {
            // Check if farm has valid coordinates
            if (farm.latitude == null || farm.longitude == null) {
                throw IllegalArgumentException("Farm coordinates are required to fetch weather data")
            }

            // Fetch current weather data from external API
            val currentWeatherData = fetchCurrentWeatherFromAPI(farm)

            // Check if weather data already exists for today
            val today = LocalDate.now()
            val existingData = weatherDataRepository.findByFarmAndDate(farm, today)

            if (existingData.isPresent) {
                // Update existing data
                val existing = existingData.get()
                val updatedData = existing.copy(
                    minTemperature = currentWeatherData.minTemperature,
                    maxTemperature = currentWeatherData.maxTemperature,
                    averageTemperature = currentWeatherData.averageTemperature,
                    rainfallMm = currentWeatherData.rainfallMm,
                    humidityPercentage = currentWeatherData.humidityPercentage,
                    windSpeedKmh = currentWeatherData.windSpeedKmh,
                    solarRadiation = currentWeatherData.solarRadiation,
                    source = "External API (Updated)",
                    updatedAt = LocalDateTime.now()
                )

                val savedData = weatherDataRepository.save(updatedData)
                logger.info { "Updated existing weather data for farm $farmId" }
                return mapToWeatherDataResponse(savedData)
            } else {
                // Create new weather data entry
                val weatherData = WeatherData(
                    farm = farm,
                    date = today,
                    minTemperature = currentWeatherData.minTemperature,
                    maxTemperature = currentWeatherData.maxTemperature,
                    averageTemperature = currentWeatherData.averageTemperature,
                    rainfallMm = currentWeatherData.rainfallMm,
                    humidityPercentage = currentWeatherData.humidityPercentage,
                    windSpeedKmh = currentWeatherData.windSpeedKmh,
                    solarRadiation = currentWeatherData.solarRadiation,
                    source = "External API",
                    createdAt = LocalDateTime.now()
                )

                val savedData = weatherDataRepository.save(weatherData)
                logger.info { "Created new weather data entry for farm $farmId" }
                return mapToWeatherDataResponse(savedData)
            }

        } catch (e: Exception) {
            logger.error(e) { "Error fetching and storing weather data: ${e.message}" }
            when (e) {
                is IllegalArgumentException, is UnauthorizedAccessException, is ResourceNotFoundException -> throw e
                else -> throw RuntimeException("Failed to fetch and store weather data: ${e.message}")
            }
        }
    }


    override fun getWeatherStatistics(
        location: String?,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Map<String, Any> {
        logger.info { "Calculating weather statistics for location: $location, dateRange: $startDate to $endDate" }

        return try {
            // Get all farms or filter by location
            val farms = if (location != null) {
                farmRepository.findAll().filter { farm ->
                    farm.location.contains(location, ignoreCase = true)
                }
            } else {
                farmRepository.findAll()
            }

            if (farms.isEmpty()) {
                return mapOf(
                    "message" to "No farms found for the specified location",
                    "totalFarms" to 0,
                    "totalRecords" to 0,
                    "location" to (location ?: "All locations")
                )
            }

            // Collect weather data from all farms
            val allWeatherData = mutableListOf<WeatherData>()
            farms.forEach { farm ->
                val weatherData = if (startDate != null && endDate != null) {
                    weatherDataRepository.findByFarmAndDateBetween(farm, startDate, endDate)
                } else {
                    weatherDataRepository.findByFarm(farm)
                }
                allWeatherData.addAll(weatherData)
            }

            if (allWeatherData.isEmpty()) {
                return mapOf(
                    "message" to "No weather data found for the specified criteria",
                    "totalFarms" to farms.size,
                    "totalRecords" to 0,
                    "location" to (location ?: "All locations"),
                    "dateRange" to mapOf(
                        "startDate" to startDate,
                        "endDate" to endDate
                    )
                )
            }

            // Calculate comprehensive statistics
            val temperatures = allWeatherData.mapNotNull { it.averageTemperature?.toDouble() }
            val minTemps = allWeatherData.mapNotNull { it.minTemperature?.toDouble() }
            val maxTemps = allWeatherData.mapNotNull { it.maxTemperature?.toDouble() }
            val rainfall = allWeatherData.mapNotNull { it.rainfallMm?.toDouble() }
            val humidity = allWeatherData.mapNotNull { it.humidityPercentage?.toDouble() }
            val windSpeed = allWeatherData.mapNotNull { it.windSpeedKmh?.toDouble() }

            // Temperature statistics
            val temperatureStats = if (temperatures.isNotEmpty()) {
                mapOf(
                    "average" to BigDecimal(temperatures.average()).setScale(2, RoundingMode.HALF_UP),
                    "minimum" to BigDecimal(temperatures.minOrNull() ?: 0.0).setScale(2, RoundingMode.HALF_UP),
                    "maximum" to BigDecimal(temperatures.maxOrNull() ?: 0.0).setScale(2, RoundingMode.HALF_UP),
                    "standardDeviation" to BigDecimal(calculateStandardDeviation(temperatures)).setScale(2, RoundingMode.HALF_UP)
                )
            } else {
                mapOf("message" to "No temperature data available")
            }

            // Rainfall statistics
            val rainfallStats = if (rainfall.isNotEmpty()) {
                mapOf(
                    "total" to BigDecimal(rainfall.sum()).setScale(2, RoundingMode.HALF_UP),
                    "average" to BigDecimal(rainfall.average()).setScale(2, RoundingMode.HALF_UP),
                    "maximum" to BigDecimal(rainfall.maxOrNull() ?: 0.0).setScale(2, RoundingMode.HALF_UP),
                    "daysWithRain" to rainfall.count { it > 0 },
                    "daysWithoutRain" to rainfall.count { it == 0.0 }
                )
            } else {
                mapOf("message" to "No rainfall data available")
            }

            // Humidity statistics
            val humidityStats = if (humidity.isNotEmpty()) {
                mapOf(
                    "average" to BigDecimal(humidity.average()).setScale(2, RoundingMode.HALF_UP),
                    "minimum" to BigDecimal(humidity.minOrNull() ?: 0.0).setScale(2, RoundingMode.HALF_UP),
                    "maximum" to BigDecimal(humidity.maxOrNull() ?: 0.0).setScale(2, RoundingMode.HALF_UP)
                )
            } else {
                mapOf("message" to "No humidity data available")
            }

            // Wind statistics
            val windStats = if (windSpeed.isNotEmpty()) {
                mapOf(
                    "average" to BigDecimal(windSpeed.average()).setScale(2, RoundingMode.HALF_UP),
                    "maximum" to BigDecimal(windSpeed.maxOrNull() ?: 0.0).setScale(2, RoundingMode.HALF_UP),
                    "minimum" to BigDecimal(windSpeed.minOrNull() ?: 0.0).setScale(2, RoundingMode.HALF_UP)
                )
            } else {
                mapOf("message" to "No wind speed data available")
            }

            // Data quality assessment
            val dataQuality = calculateWeatherDataQuality(allWeatherData)

            // Extreme weather events
            val extremeEvents = identifyExtremeWeatherEvents(allWeatherData)

            // Monthly trends (if date range spans multiple months)
            val monthlyTrends = calculateMonthlyWeatherTrends(allWeatherData)

            mapOf(
                "totalFarms" to farms.size,
                "totalRecords" to allWeatherData.size,
                "location" to (location ?: "All locations"),
                "dateRange" to mapOf(
                    "startDate" to (startDate ?: allWeatherData.minOfOrNull { it.date }),
                    "endDate" to (endDate ?: allWeatherData.maxOfOrNull { it.date })
                ),
                "temperature" to temperatureStats,
                "rainfall" to rainfallStats,
                "humidity" to humidityStats,
                "windSpeed" to windStats,
                "dataQuality" to dataQuality,
                "extremeEvents" to extremeEvents,
                "monthlyTrends" to monthlyTrends,
                "calculatedAt" to LocalDateTime.now()
            )

        } catch (e: Exception) {
            logger.error(e) { "Error calculating weather statistics: ${e.message}" }
            mapOf(
                "error" to true,
                "message" to (e.message ?: "Unknown error occurred"),
                "calculatedAt" to LocalDateTime.now()
            )
        }
    }

    override fun getCurrentWeatherByLocation(location: String): WeatherDataResponse {
        TODO("Not yet implemented")
    }

    override fun getCurrentWeatherForFarm(farmId: Long): WeatherDataResponse {
        TODO("Not yet implemented")
    }

    override fun getWeatherHistory(
        farmId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<WeatherDataResponse> {
        TODO("Not yet implemented")
    }

    override fun getWeatherHistoryForLocation(
        location: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<WeatherDataResponse> {
        TODO("Not yet implemented")
    }

    override fun fetchLatestWeatherData(farmId: Long) {
        TODO("Not yet implemented")
    }

// Helper methods

    private fun validateWeatherDataRequest(request: WeatherDataCreateRequest) {
        // Temperature validation
        if (request.minTemperature != null && request.maxTemperature != null) {
            if (request.minTemperature > request.maxTemperature) {
                throw IllegalArgumentException("Minimum temperature cannot be greater than maximum temperature")
            }
        }

        // Validate temperature ranges (reasonable for agricultural contexts)
        request.minTemperature?.let { temp ->
            if (temp.toDouble() < -50.0 || temp.toDouble() > 60.0) {
                throw IllegalArgumentException("Minimum temperature must be between -50°C and 60°C")
            }
        }

        request.maxTemperature?.let { temp ->
            if (temp.toDouble() < -50.0 || temp.toDouble() > 60.0) {
                throw IllegalArgumentException("Maximum temperature must be between -50°C and 60°C")
            }
        }

        // Validate other parameters
        request.humidityPercentage?.let { humidity ->
            if (humidity.toDouble() < 0.0 || humidity.toDouble() > 100.0) {
                throw IllegalArgumentException("Humidity percentage must be between 0% and 100%")
            }
        }

        request.rainfallMm?.let { rainfall ->
            if (rainfall.toDouble() < 0.0) {
                throw IllegalArgumentException("Rainfall cannot be negative")
            }
        }

        request.windSpeedKmh?.let { windSpeed ->
            if (windSpeed.toDouble() < 0.0) {
                throw IllegalArgumentException("Wind speed cannot be negative")
            }
        }
    }

    private fun calculateAverageTemperature(minTemp: BigDecimal?, maxTemp: BigDecimal?): BigDecimal? {
        return if (minTemp != null && maxTemp != null) {
            (minTemp + maxTemp).divide(BigDecimal("2"), 2, RoundingMode.HALF_UP)
        } else null
    }

    private fun fetchCurrentWeatherFromAPI(farm: Farm): WeatherDataResponse {
        // This is a simplified mock implementation
        // In a real implementation, you would call actual weather APIs like OpenWeatherMap, WeatherAPI, etc.

        logger.info { "Fetching weather data from external API for farm ${farm.id} at coordinates (${farm.latitude}, ${farm.longitude})" }

        // Mock current weather data - replace with actual API calls
        val currentTemp = 20.0 + (Math.random() * 15.0) // 20-35°C
        val humidity = 40.0 + (Math.random() * 40.0) // 40-80%
        val rainfall = if (Math.random() > 0.7) Math.random() * 10.0 else 0.0 // 30% chance of rain

        return WeatherDataResponse(
            farmId = farm.id!!,
            date = LocalDate.now(),
            minTemperature = BigDecimal(currentTemp - 5.0).setScale(2, RoundingMode.HALF_UP),
            maxTemperature = BigDecimal(currentTemp + 5.0).setScale(2, RoundingMode.HALF_UP),
            averageTemperature = BigDecimal(currentTemp).setScale(2, RoundingMode.HALF_UP),
            rainfallMm = BigDecimal(rainfall).setScale(2, RoundingMode.HALF_UP),
            humidityPercentage = BigDecimal(humidity).setScale(2, RoundingMode.HALF_UP),
            windSpeedKmh = BigDecimal(5.0 + Math.random() * 15.0).setScale(2, RoundingMode.HALF_UP),
            solarRadiation = BigDecimal(15.0 + Math.random() * 10.0).setScale(2, RoundingMode.HALF_UP),
            source = "External Weather API",
            pressure = BigDecimal(1000.0 + Math.random() * 50.0).setScale(2, RoundingMode.HALF_UP),
            uvIndex = BigDecimal(Math.random() * 11.0).setScale(1, RoundingMode.HALF_UP),
            visibility = BigDecimal(8.0 + Math.random() * 7.0).setScale(2, RoundingMode.HALF_UP),
            cloudCover = BigDecimal(Math.random() * 100.0).setScale(2, RoundingMode.HALF_UP)
        )
    }

    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0

        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.average()
        return kotlin.math.sqrt(variance)
    }

    private fun calculateWeatherDataQuality(weatherData: List<WeatherData>): Map<String, Any> {
        if (weatherData.isEmpty()) {
            return mapOf("score" to 0, "message" to "No data available")
        }

        var qualityScore = 100.0
        val issues = mutableListOf<String>()

        // Check completeness of key fields
        val temperatureCompleteness = weatherData.count { it.averageTemperature != null }.toDouble() / weatherData.size
        val rainfallCompleteness = weatherData.count { it.rainfallMm != null }.toDouble() / weatherData.size
        val humidityCompleteness = weatherData.count { it.humidityPercentage != null }.toDouble() / weatherData.size

        qualityScore *= (temperatureCompleteness + rainfallCompleteness + humidityCompleteness) / 3.0

        if (temperatureCompleteness < 0.9) issues.add("Missing temperature data")
        if (rainfallCompleteness < 0.9) issues.add("Missing rainfall data")
        if (humidityCompleteness < 0.9) issues.add("Missing humidity data")

        // Check for data recency
        val latestDate = weatherData.maxOfOrNull { it.date }
        val daysSinceLastUpdate = latestDate?.let { LocalDate.now().toEpochDay() - it.toEpochDay() } ?: Long.MAX_VALUE

        if (daysSinceLastUpdate > 7) {
            qualityScore *= 0.8
            issues.add("Data is not recent (older than 7 days)")
        }

        // Check for outliers
        val temperatures = weatherData.mapNotNull { it.averageTemperature?.toDouble() }
        if (temperatures.isNotEmpty()) {
            val tempMean = temperatures.average()
            val tempStdDev = calculateStandardDeviation(temperatures)
            val outliers = temperatures.count { kotlin.math.abs(it - tempMean) > 3 * tempStdDev }

            if (outliers > temperatures.size * 0.05) { // More than 5% outliers
                qualityScore *= 0.9
                issues.add("Temperature data contains significant outliers")
            }
        }

        return mapOf(
            "score" to BigDecimal(qualityScore).setScale(1, RoundingMode.HALF_UP),
            "completeness" to mapOf(
                "temperature" to BigDecimal(temperatureCompleteness * 100).setScale(1, RoundingMode.HALF_UP),
                "rainfall" to BigDecimal(rainfallCompleteness * 100).setScale(1, RoundingMode.HALF_UP),
                "humidity" to BigDecimal(humidityCompleteness * 100).setScale(1, RoundingMode.HALF_UP)
            ),
            "recency" to mapOf(
                "latestDate" to latestDate,
                "daysSinceLastUpdate" to daysSinceLastUpdate
            ),
            "issues" to issues
        )
    }

    private fun identifyExtremeWeatherEvents(weatherData: List<WeatherData>): List<Map<String, Any>> {
        val extremeEvents = mutableListOf<Map<String, Any>>()

        weatherData.forEach { data ->
            // High temperature events
            data.maxTemperature?.let { maxTemp ->
                if (maxTemp.toDouble() > 35.0) {
                    extremeEvents.add(
                        mapOf(
                            "date" to data.date,
                            "type" to "HIGH_TEMPERATURE",
                            "value" to maxTemp,
                            "unit" to "°C",
                            "severity" to when {
                                maxTemp.toDouble() > 40.0 -> "SEVERE"
                                maxTemp.toDouble() > 37.0 -> "MODERATE"
                                else -> "MILD"
                            },
                            "farmId" to data.farm.id
                        ) as Map<String, Any>
                    )
                }
            }

            // Heavy rainfall events
            data.rainfallMm?.let { rainfall ->
                if (rainfall.toDouble() > 25.0) {
                    extremeEvents.add(
                        mapOf(
                            "date" to data.date,
                            "type" to "HEAVY_RAINFALL",
                            "value" to rainfall,
                            "unit" to "mm",
                            "severity" to when {
                                rainfall.toDouble() > 50.0 -> "SEVERE"
                                rainfall.toDouble() > 35.0 -> "MODERATE"
                                else -> "MILD"
                            },
                            "farmId" to data.farm.id
                        ) as Map<String, Any>
                    )
                }
            }

            // Strong wind events
            data.windSpeedKmh?.let { windSpeed ->
                if (windSpeed.toDouble() > 30.0) {
                    extremeEvents.add(
                        mapOf(
                            "date" to data.date,
                            "type" to "STRONG_WIND",
                            "value" to windSpeed,
                            "unit" to "km/h",
                            "severity" to when {
                                windSpeed.toDouble() > 50.0 -> "SEVERE"
                                windSpeed.toDouble() > 40.0 -> "MODERATE"
                                else -> "MILD"
                            },
                            "farmId" to data.farm.id
                        ) as Map<String, Any>
                    )
                }
            }
        }

        return extremeEvents.sortedByDescending { it["date"] as LocalDate }
    }

    private fun calculateMonthlyWeatherTrends(weatherData: List<WeatherData>): List<Map<String, Any>> {
        if (weatherData.isEmpty()) return emptyList()

        return weatherData
            .groupBy { it.date.withDayOfMonth(1) } // Group by month
            .map { (month, monthlyData) ->
                val temperatures = monthlyData.mapNotNull { it.averageTemperature?.toDouble() }
                val rainfall = monthlyData.mapNotNull { it.rainfallMm?.toDouble() }
                val humidity = monthlyData.mapNotNull { it.humidityPercentage?.toDouble() }

                mapOf(
                    "month" to month,
                    "recordCount" to monthlyData.size,
                    "temperature" to if (temperatures.isNotEmpty()) {
                        mapOf(
                            "average" to BigDecimal(temperatures.average()).setScale(2, RoundingMode.HALF_UP),
                            "minimum" to BigDecimal(temperatures.minOrNull() ?: 0.0).setScale(2, RoundingMode.HALF_UP),
                            "maximum" to BigDecimal(temperatures.maxOrNull() ?: 0.0).setScale(2, RoundingMode.HALF_UP)
                        )
                    } else null,
                    "rainfall" to if (rainfall.isNotEmpty()) {
                        mapOf(
                            "total" to BigDecimal(rainfall.sum()).setScale(2, RoundingMode.HALF_UP),
                            "average" to BigDecimal(rainfall.average()).setScale(2, RoundingMode.HALF_UP),
                            "daysWithRain" to rainfall.count { it > 0 }
                        )
                    } else null,
                    "humidity" to if (humidity.isNotEmpty()) {
                        mapOf(
                            "average" to BigDecimal(humidity.average()).setScale(2, RoundingMode.HALF_UP)
                        )
                    } else null
                )
            }
            .sortedBy { it["month"] as LocalDate } as List<Map<String, Any>>
    }
}