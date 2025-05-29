package com.maizeyield.scheduler

import com.maizeyield.model.WeatherData
import com.maizeyield.repository.FarmRepository
import com.maizeyield.repository.WeatherDataRepository
import com.maizeyield.service.external.WeatherApiService
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Component
@ConditionalOnProperty(value = ["weather.auto-fetch.enabled"], havingValue = "true", matchIfMissing = false)
class WeatherScheduler(
    private val weatherApiService: WeatherApiService,
    private val farmRepository: FarmRepository,
    private val weatherDataRepository: WeatherDataRepository
) {

    /**
     * Automatically fetch weather data for all farms daily at 6 AM
     */
    @Scheduled(cron = "0 0 6 * * ?") // 6 AM daily
    @Transactional
    fun fetchDailyWeatherData() {
        logger.info { "Starting scheduled weather data fetch for all farms" }

        try {
            val farms = farmRepository.findAll()
            var successCount = 0
            var errorCount = 0

            farms.forEach { farm ->
                try {
                    // Check if we already have weather data for today
                    val today = LocalDate.now()
                    val existingData = weatherDataRepository.findByFarmAndDate(farm, today)

                    if (existingData.isEmpty) {
                        // Fetch current weather data
                        val weatherResponse = weatherApiService.fetchCurrentWeather(farm)

                        weatherResponse?.let { response ->
                            val weatherData = WeatherData(
                                farm = farm,
                                date = response.date,
                                minTemperature = response.minTemperature,
                                maxTemperature = response.maxTemperature,
                                averageTemperature = response.averageTemperature,
                                rainfallMm = response.rainfallMm,
                                humidityPercentage = response.humidityPercentage,
                                windSpeedKmh = response.windSpeedKmh,
                                solarRadiation = response.solarRadiation,
                                source = response.source
                            )

                            weatherDataRepository.save(weatherData)
                            successCount++
                            logger.debug { "Successfully fetched weather data for farm: ${farm.name}" }
                        } ?: run {
                            errorCount++
                            logger.warn { "Failed to fetch weather data for farm: ${farm.name}" }
                        }
                    } else {
                        logger.debug { "Weather data already exists for farm: ${farm.name} on date: $today" }
                    }
                } catch (e: Exception) {
                    errorCount++
                    logger.error(e) { "Error fetching weather data for farm: ${farm.name}" }
                }
            }

            logger.info { "Completed scheduled weather fetch - Success: $successCount, Errors: $errorCount" }

        } catch (e: Exception) {
            logger.error(e) { "Error in scheduled weather data fetch" }
        }
    }

    /**
     * Fetch historical weather data for farms that are missing data
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    @Transactional
    fun fetchMissingHistoricalData() {
        logger.info { "Starting scheduled historical weather data fetch" }

        try {
            val farms = farmRepository.findAll()
            val endDate = LocalDate.now().minusDays(1)
            val startDate = endDate.minusDays(7) // Look back 7 days

            farms.forEach { farm ->
                try {
                    // Find missing dates in the range
                    val existingDates = weatherDataRepository
                        .findByFarmAndDateBetween(farm, startDate, endDate)
                        .map { it.date }
                        .toSet()

                    val allDates = generateSequence(startDate) { it.plusDays(1) }
                        .takeWhile { it <= endDate }
                        .toList()

                    val missingDates = allDates.filterNot { it in existingDates }

                    if (missingDates.isNotEmpty()) {
                        logger.info { "Fetching missing weather data for farm: ${farm.name}, dates: $missingDates" }

                        missingDates.forEach { date ->
                            try {
                                val weatherResponse = weatherApiService.fetchHistoricalWeather(farm, date)

                                weatherResponse?.let { response ->
                                    val weatherData = WeatherData(
                                        farm = farm,
                                        date = response.date,
                                        minTemperature = response.minTemperature,
                                        maxTemperature = response.maxTemperature,
                                        averageTemperature = response.averageTemperature,
                                        rainfallMm = response.rainfallMm,
                                        humidityPercentage = response.humidityPercentage,
                                        windSpeedKmh = response.windSpeedKmh,
                                        solarRadiation = response.solarRadiation,
                                        source = response.source
                                    )

                                    weatherDataRepository.save(weatherData)
                                    logger.debug { "Fetched historical weather data for farm: ${farm.name} on date: $date" }
                                }

                                // Add small delay to avoid API rate limiting
                                Thread.sleep(100)

                            } catch (e: Exception) {
                                logger.error(e) { "Error fetching historical weather data for farm: ${farm.name} on date: $date" }
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error processing historical weather data for farm: ${farm.name}" }
                }
            }

        } catch (e: Exception) {
            logger.error(e) { "Error in scheduled historical weather data fetch" }
        }
    }

    /**
     * Clean up old weather data to prevent database bloat
     */
    @Scheduled(cron = "0 0 3 1 * ?") // 3 AM on the 1st of each month
    @Transactional
    fun cleanupOldWeatherData() {
        logger.info { "Starting weather data cleanup" }

        try {
            val cutoffDate = LocalDate.now().minusYears(2) // Keep 2 years of data
            val farms = farmRepository.findAll()

            farms.forEach { farm ->
                try {
                    val oldRecords = weatherDataRepository.findByFarmAndDateBefore(farm, cutoffDate)
                    if (oldRecords.isNotEmpty()) {
                        weatherDataRepository.deleteAll(oldRecords)
                        logger.info { "Deleted ${oldRecords.size} old weather records for farm: ${farm.name}" }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error cleaning up weather data for farm: ${farm.name}" }
                }
            }

        } catch (e: Exception) {
            logger.error(e) { "Error in weather data cleanup" }
        }
    }
}