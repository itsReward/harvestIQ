package com.maizeyield.config

import com.maizeyield.dto.WeatherDataResponse
import com.maizeyield.model.Farm
import com.maizeyield.service.external.WeatherAlert
import com.maizeyield.service.external.WeatherApiService
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.EnableRetry
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Configuration
@EnableRetry
class WeatherServiceConfig {

    @Bean
    @Primary
    fun resilientWeatherService(
        weatherApiService: WeatherApiService,
        @Value("\${weather.fallback.enabled:true}") fallbackEnabled: Boolean,
        @Value("\${weather.fallback.providers:weatherapi,openweather}") fallbackProviders: String
    ): ResilientWeatherService {
        return ResilientWeatherService(weatherApiService, fallbackEnabled, fallbackProviders.split(","))
    }
}

@Service
class ResilientWeatherService(
    private val primaryWeatherService: WeatherApiService,
    private val fallbackEnabled: Boolean,
    private val fallbackProviders: List<String>
) {

    @Retryable(
        value = [Exception::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2.0)
    )
    fun fetchCurrentWeatherWithFallback(farm: Farm): WeatherDataResponse? {
        return try {
            logger.info { "Attempting to fetch current weather for farm: ${farm.name}" }
            primaryWeatherService.fetchCurrentWeather(farm)
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
    fun fetchWeatherForecastWithFallback(farm: Farm, days: Int): List<WeatherDataResponse> {
        return try {
            logger.info { "Attempting to fetch weather forecast for farm: ${farm.name}" }
            primaryWeatherService.fetchWeatherForecast(farm, days)
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
    fun fetchHistoricalWeatherWithFallback(farm: Farm, date: LocalDate): WeatherDataResponse? {
        return try {
            logger.info { "Attempting to fetch historical weather for farm: ${farm.name} on $date" }
            primaryWeatherService.fetchHistoricalWeather(farm, date)
        } catch (e: Exception) {
            logger.warn(e) { "Primary weather service failed for historical data for farm: ${farm.name}" }
            if (fallbackEnabled) {
                attemptFallbackHistorical(farm, date)
            } else {
                null
            }
        }
    }

    fun fetchWeatherAlertsWithFallback(farm: Farm): List<WeatherAlert> {
        return try {
            primaryWeatherService.fetchWeatherAlerts(farm)
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
                val fallbackService = createFallbackService(provider)
                val result = fallbackService?.fetchCurrentWeather(farm)
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
                val fallbackService = createFallbackService(provider)
                val result = fallbackService?.fetchWeatherForecast(farm, days)
                if (!result.isNullOrEmpty()) {
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
                val fallbackService = createFallbackService(provider)
                val result = fallbackService?.fetchHistoricalWeather(farm, date)
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

    private fun createFallbackService(provider: String): WeatherApiService? {
        // In a real implementation, you would create different service instances
        // based on the provider. For simplicity, we're returning null here
        // but you could implement provider-specific services
        return when (provider.lowercase()) {
            "weatherapi" -> primaryWeatherService
            "openweather" -> primaryWeatherService // Would be a different instance
            "weatherstack" -> primaryWeatherService // Would be a different instance
            else -> null
        }
    }
}

@Service
class WeatherDataValidator {

    fun validateWeatherData(data: WeatherDataResponse): Boolean {
        return try {
            // Basic validation rules
            val isValid = data.averageTemperature?.let { temp ->
                temp.toDouble() in -60.0..60.0 // Reasonable temperature range
            } ?: true &&
                    data.rainfallMm?.let { rainfall ->
                        rainfall.toDouble() >= 0.0 && rainfall.toDouble() <= 1000.0 // Reasonable daily rainfall
                    } ?: true &&
                    data.humidityPercentage?.let { humidity ->
                        humidity.toDouble() in 0.0..100.0
                    } ?: true &&
                    data.windSpeedKmh?.let { wind ->
                        wind.toDouble() in 0.0..500.0 // Maximum recorded wind speed
                    } ?: true

            if (!isValid) {
                logger.warn { "Weather data validation failed for farm ${data.farmId} on ${data.date}" }
            }

            isValid
        } catch (e: Exception) {
            logger.error(e) { "Error validating weather data for farm ${data.farmId}" }
            false
        }
    }

    fun sanitizeWeatherData(data: WeatherDataResponse): WeatherDataResponse {
        return data.copy(
            averageTemperature = data.averageTemperature?.let { temp ->
                if (temp.toDouble() in -60.0..60.0) temp else null
            },
            rainfallMm = data.rainfallMm?.let { rainfall ->
                if (rainfall.toDouble() >= 0.0 && rainfall.toDouble() <= 1000.0) rainfall else null
            },
            humidityPercentage = data.humidityPercentage?.let { humidity ->
                if (humidity.toDouble() in 0.0..100.0) humidity else null
            },
            windSpeedKmh = data.windSpeedKmh?.let { wind ->
                if (wind.toDouble() in 0.0..500.0) wind else null
            }
        )
    }
}