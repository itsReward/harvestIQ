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