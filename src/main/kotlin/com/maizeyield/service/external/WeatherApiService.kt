package com.maizeyield.service.external

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.maizeyield.dto.WeatherDataResponse
import com.maizeyield.model.Farm
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}

@Service
class WeatherApiService(
    private val webClient: WebClient,
    @Value("\${weather.api.key}") private val apiKey: String,
    @Value("\${weather.api.base-url}") private val baseUrl: String
) {

    /**
     * Fetch current weather data from external API
     */
    fun fetchCurrentWeather(farm: Farm): WeatherDataResponse? {
        return try {
            logger.info { "Fetching current weather for farm: ${farm.name} at coordinates: ${farm.latitude}, ${farm.longitude}" }

            val response = webClient.get()
                .uri("$baseUrl/current.json") { uriBuilder ->
                    uriBuilder
                        .queryParam("key", apiKey)
                        .queryParam("q", "${farm.latitude},${farm.longitude}")
                        .queryParam("aqi", "no")
                        .build()
                }
                .retrieve()
                .bodyToMono(WeatherApiCurrentResponse::class.java)
                .block()

            response?.let { convertCurrentWeatherToWeatherData(it, farm) }

        } catch (e: WebClientResponseException) {
            logger.error { "Weather API error: ${e.statusCode} - ${e.responseBodyAsString}" }
            null
        } catch (e: Exception) {
            logger.error(e) { "Error fetching current weather for farm: ${farm.name}" }
            null
        }
    }

    /**
     * Fetch weather forecast for multiple days
     */
    fun fetchWeatherForecast(farm: Farm, days: Int = 7): List<WeatherDataResponse> {
        return try {
            logger.info { "Fetching ${days}-day weather forecast for farm: ${farm.name}" }

            val response = webClient.get()
                .uri("$baseUrl/forecast.json") { uriBuilder ->
                    uriBuilder
                        .queryParam("key", apiKey)
                        .queryParam("q", "${farm.latitude},${farm.longitude}")
                        .queryParam("days", days.coerceAtMost(10)) // API limit
                        .queryParam("aqi", "no")
                        .queryParam("alerts", "no")
                        .build()
                }
                .retrieve()
                .bodyToMono(WeatherApiForecastResponse::class.java)
                .block()

            response?.forecast?.forecastday?.map { forecastDay ->
                convertForecastDayToWeatherData(forecastDay, farm)
            } ?: emptyList()

        } catch (e: WebClientResponseException) {
            logger.error { "Weather forecast API error: ${e.statusCode} - ${e.responseBodyAsString}" }
            emptyList()
        } catch (e: Exception) {
            logger.error(e) { "Error fetching weather forecast for farm: ${farm.name}" }
            emptyList()
        }
    }

    /**
     * Fetch historical weather data
     */
    fun fetchHistoricalWeather(farm: Farm, date: LocalDate): WeatherDataResponse? {
        return try {
            logger.info { "Fetching historical weather for farm: ${farm.name} on date: $date" }

            val response = webClient.get()
                .uri("$baseUrl/history.json") { uriBuilder ->
                    uriBuilder
                        .queryParam("key", apiKey)
                        .queryParam("q", "${farm.latitude},${farm.longitude}")
                        .queryParam("dt", date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                        .build()
                }
                .retrieve()
                .bodyToMono(WeatherApiHistoryResponse::class.java)
                .block()

            response?.forecast?.forecastday?.firstOrNull()?.let { historyDay ->
                convertForecastDayToWeatherData(historyDay, farm)
            }

        } catch (e: WebClientResponseException) {
            logger.error { "Historical weather API error: ${e.statusCode} - ${e.responseBodyAsString}" }
            null
        } catch (e: Exception) {
            logger.error(e) { "Error fetching historical weather for farm: ${farm.name} on date: $date" }
            null
        }
    }

    /**
     * Fetch weather alerts for the farm location
     */
    fun fetchWeatherAlerts(farm: Farm): List<WeatherAlert> {
        return try {
            logger.info { "Fetching weather alerts for farm: ${farm.name}" }

            val response = webClient.get()
                .uri("$baseUrl/forecast.json") { uriBuilder ->
                    uriBuilder
                        .queryParam("key", apiKey)
                        .queryParam("q", "${farm.latitude},${farm.longitude}")
                        .queryParam("days", 1)
                        .queryParam("aqi", "no")
                        .queryParam("alerts", "yes")
                        .build()
                }
                .retrieve()
                .bodyToMono(WeatherApiForecastResponse::class.java)
                .block()

            response?.alerts?.alert?.map { alert ->
                WeatherAlert(
                    headline = alert.headline,
                    description = alert.desc,
                    severity = alert.severity,
                    urgency = alert.urgency,
                    areas = alert.areas,
                    effective = alert.effective,
                    expires = alert.expires
                )
            } ?: emptyList()

        } catch (e: Exception) {
            logger.error(e) { "Error fetching weather alerts for farm: ${farm.name}" }
            emptyList()
        }
    }

    private fun convertCurrentWeatherToWeatherData(response: WeatherApiCurrentResponse, farm: Farm): WeatherDataResponse {
        return WeatherDataResponse(
            farmId = farm.id!!,
            date = LocalDate.now(),
            minTemperature = null, // Current weather doesn't have min/max
            maxTemperature = null,
            averageTemperature = response.current.tempC?.let { BigDecimal(it.toString()) },
            rainfallMm = response.current.precipMm?.let { BigDecimal(it.toString()) },
            humidityPercentage = response.current.humidity?.let { BigDecimal(it.toString()) },
            windSpeedKmh = response.current.windKph?.let { BigDecimal(it.toString()) },
            pressure = response.current.pressureMb?.let { BigDecimal(it.toString()) },
            uvIndex = response.current.uv?.let { BigDecimal(it.toString()) },
            visibility = response.current.visKm?.let { BigDecimal(it.toString()) },
            cloudCover = response.current.cloud?.let { BigDecimal(it.toString()) },
            source = "WeatherAPI",
        )
    }

    private fun convertForecastDayToWeatherData(forecastDay: ForecastDay, farm: Farm): WeatherDataResponse {
        return WeatherDataResponse(
            farmId = farm.id!!,
            date = LocalDate.parse(forecastDay.date),
            minTemperature = forecastDay.day.mintempC?.let { BigDecimal(it.toString()) },
            maxTemperature = forecastDay.day.maxtempC?.let { BigDecimal(it.toString()) },
            averageTemperature = forecastDay.day.avgtempC?.let { BigDecimal(it.toString()) },
            rainfallMm = forecastDay.day.totalprecipMm?.let { BigDecimal(it.toString()) },
            humidityPercentage = forecastDay.day.avghumidity?.let { BigDecimal(it.toString()) },
            windSpeedKmh = forecastDay.day.maxwindKph?.let { BigDecimal(it.toString()) },
            uvIndex = forecastDay.day.uv?.let { BigDecimal(it.toString()) },
            source = "WeatherAPI"
        )
    }
}

// Data classes for WeatherAPI responses
@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherApiCurrentResponse(
    val current: Current
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherApiForecastResponse(
    val forecast: Forecast,
    val alerts: Alerts?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherApiHistoryResponse(
    val forecast: Forecast
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Current(
    @JsonProperty("temp_c") val tempC: Double?,
    @JsonProperty("precip_mm") val precipMm: Double?,
    val humidity: Int?,
    @JsonProperty("wind_kph") val windKph: Double?,
    @JsonProperty("pressure_mb") val pressureMb: Double?,
    val uv: Double?,
    @JsonProperty("vis_km") val visKm: Double?,
    val cloud: Int?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Forecast(
    val forecastday: List<ForecastDay>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ForecastDay(
    val date: String,
    val day: Day
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Day(
    @JsonProperty("mintemp_c") val mintempC: Double?,
    @JsonProperty("maxtemp_c") val maxtempC: Double?,
    @JsonProperty("avgtemp_c") val avgtempC: Double?,
    @JsonProperty("totalprecip_mm") val totalprecipMm: Double?,
    val avghumidity: Int?,
    @JsonProperty("maxwind_kph") val maxwindKph: Double?,
    val uv: Double?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Alerts(
    val alert: List<Alert>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Alert(
    val headline: String,
    val desc: String,
    val severity: String,
    val urgency: String,
    val areas: String,
    val effective: String,
    val expires: String
)

data class WeatherAlert(
    val headline: String,
    val description: String,
    val severity: String,
    val urgency: String,
    val areas: String,
    val effective: String,
    val expires: String
)