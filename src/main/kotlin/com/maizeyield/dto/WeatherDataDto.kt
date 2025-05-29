package com.maizeyield.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal
import java.time.LocalDate

// Request DTOs
data class WeatherDataCreateRequest(
    val farmId: Long,
    val date: LocalDate,
    val minTemperature: BigDecimal? = null,
    val maxTemperature: BigDecimal? = null,
    val averageTemperature: BigDecimal? = null,
    val rainfallMm: BigDecimal? = null,
    val humidityPercentage: BigDecimal? = null,
    val windSpeedKmh: BigDecimal? = null,
    val solarRadiation: BigDecimal? = null,
    val source: String
)

data class WeatherDataUpdateRequest(
    val minTemperature: BigDecimal? = null,
    val maxTemperature: BigDecimal? = null,
    val averageTemperature: BigDecimal? = null,
    val rainfallMm: BigDecimal? = null,
    val humidityPercentage: BigDecimal? = null,
    val windSpeedKmh: BigDecimal? = null,
    val solarRadiation: BigDecimal? = null,
    val source: String? = null
)

// Response DTOs
@JsonInclude(JsonInclude.Include.NON_NULL)
data class WeatherDataResponse(
    val id: Long? = null,
    val farmId: Long,
    val date: LocalDate,
    val minTemperature: BigDecimal? = null,
    val maxTemperature: BigDecimal? = null,
    val averageTemperature: BigDecimal? = null,
    val rainfallMm: BigDecimal? = null,
    val humidityPercentage: BigDecimal? = null,
    val windSpeedKmh: BigDecimal? = null,
    val solarRadiation: BigDecimal? = null,
    val source: String,
    val pressure: BigDecimal? = null,
    val uvIndex: BigDecimal? = null,
    val visibility: BigDecimal? = null,
    val cloudCover: BigDecimal? = null,
)


// Special DTOs for weather data retrieval from external API
data class WeatherForecastRequest(
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class WeatherForecastResponse(
    val forecasts: List<WeatherDataResponse>
)