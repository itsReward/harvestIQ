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
import java.time.LocalDateTime
import java.time.ZoneId

private val logger = KotlinLogging.logger {}

@Service
class WeatherApiService(
    private val webClient: WebClient,
    @Value("\${weather.api.provider:weatherapi}") private val provider: String,
    @Value("\${weather.api.key}") private val apiKey: String,
    @Value("\${weather.openweather.api.key:}") private val openWeatherApiKey: String,
    @Value("\${weather.api.base-url:https://api.weatherapi.com/v1}") private val baseUrl: String,
    @Value("\${weather.api.timeout:30}") private val timeoutSeconds: Long
) {

    companion object {
        const val WEATHERAPI_PROVIDER = "weatherapi"
        const val OPENWEATHER_PROVIDER = "openweather"
        const val WEATHERSTACK_PROVIDER = "weatherstack"
    }

    /**
     * Fetch current weather data from external API
     */
    fun fetchCurrentWeather(farm: Farm): WeatherDataResponse? {
        return when (provider.lowercase()) {
            WEATHERAPI_PROVIDER -> fetchWeatherApiCurrent(farm)
            OPENWEATHER_PROVIDER -> fetchOpenWeatherCurrent(farm)
            WEATHERSTACK_PROVIDER -> fetchWeatherStackCurrent(farm)
            else -> {
                logger.warn { "Unknown weather provider: $provider, falling back to WeatherAPI" }
                fetchWeatherApiCurrent(farm)
            }
        }
    }

    /**
     * Fetch weather forecast for multiple days
     */
    fun fetchWeatherForecast(farm: Farm, days: Int = 7): List<WeatherDataResponse> {
        return when (provider.lowercase()) {
            WEATHERAPI_PROVIDER -> fetchWeatherApiForecast(farm, days)
            OPENWEATHER_PROVIDER -> fetchOpenWeatherForecast(farm, days)
            WEATHERSTACK_PROVIDER -> fetchWeatherStackForecast(farm, days)
            else -> {
                logger.warn { "Unknown weather provider: $provider, falling back to WeatherAPI" }
                fetchWeatherApiForecast(farm, days)
            }
        }
    }

    /**
     * Fetch historical weather data
     */
    fun fetchHistoricalWeather(farm: Farm, date: LocalDate): WeatherDataResponse? {
        return when (provider.lowercase()) {
            WEATHERAPI_PROVIDER -> fetchWeatherApiHistorical(farm, date)
            OPENWEATHER_PROVIDER -> fetchOpenWeatherHistorical(farm, date)
            WEATHERSTACK_PROVIDER -> fetchWeatherStackHistorical(farm, date)
            else -> {
                logger.warn { "Unknown weather provider: $provider, falling back to WeatherAPI" }
                fetchWeatherApiHistorical(farm, date)
            }
        }
    }

    /**
     * Fetch weather alerts for the farm location
     */
    fun fetchWeatherAlerts(farm: Farm): List<WeatherAlert> {
        return when (provider.lowercase()) {
            WEATHERAPI_PROVIDER -> fetchWeatherApiAlerts(farm)
            OPENWEATHER_PROVIDER -> fetchOpenWeatherAlerts(farm)
            else -> {
                logger.info { "Weather alerts not supported for provider: $provider" }
                emptyList()
            }
        }
    }

    // WeatherAPI.com implementation
    private fun fetchWeatherApiCurrent(farm: Farm): WeatherDataResponse? {
        return try {
            logger.info { "Fetching current weather from WeatherAPI for farm: ${farm.name}" }

            val response = webClient.get()
                .uri("$baseUrl/current.json") { uriBuilder ->
                    uriBuilder
                        .queryParam("key", apiKey)
                        .queryParam("q", "${farm.latitude},${farm.longitude}")
                        .queryParam("aqi", "yes") // Include air quality data
                        .build()
                }
                .retrieve()
                .onStatus(HttpStatus.UNAUTHORIZED::equals) {
                    Mono.error(RuntimeException("Invalid API key for WeatherAPI"))
                }
                .onStatus(HttpStatus.FORBIDDEN::equals) {
                    Mono.error(RuntimeException("API key quota exceeded for WeatherAPI"))
                }
                .bodyToMono(WeatherApiCurrentResponse::class.java)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .block()

            response?.let { convertWeatherApiCurrentToWeatherData(it, farm) }

        } catch (e: WebClientResponseException) {
            logger.error { "WeatherAPI error: ${e.statusCode} - ${e.responseBodyAsString}" }
            null
        } catch (e: Exception) {
            logger.error(e) { "Error fetching current weather from WeatherAPI for farm: ${farm.name}" }
            null
        }
    }

    private fun fetchWeatherApiForecast(farm: Farm, days: Int): List<WeatherDataResponse> {
        return try {
            logger.info { "Fetching ${days}-day forecast from WeatherAPI for farm: ${farm.name}" }

            val response = webClient.get()
                .uri("$baseUrl/forecast.json") { uriBuilder ->
                    uriBuilder
                        .queryParam("key", apiKey)
                        .queryParam("q", "${farm.latitude},${farm.longitude}")
                        .queryParam("days", days.coerceAtMost(10))
                        .queryParam("aqi", "no")
                        .queryParam("alerts", "yes")
                        .build()
                }
                .retrieve()
                .bodyToMono(WeatherApiForecastResponse::class.java)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .block()

            response?.forecast?.forecastday?.map { forecastDay ->
                convertWeatherApiForecastToWeatherData(forecastDay, farm)
            } ?: emptyList()

        } catch (e: Exception) {
            logger.error(e) { "Error fetching forecast from WeatherAPI for farm: ${farm.name}" }
            emptyList()
        }
    }

    private fun fetchWeatherApiHistorical(farm: Farm, date: LocalDate): WeatherDataResponse? {
        return try {
            logger.info { "Fetching historical weather from WeatherAPI for farm: ${farm.name} on $date" }

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
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .block()

            response?.forecast?.forecastday?.firstOrNull()?.let { historyDay ->
                convertWeatherApiForecastToWeatherData(historyDay, farm)
            }

        } catch (e: Exception) {
            logger.error(e) { "Error fetching historical weather from WeatherAPI for farm: ${farm.name}" }
            null
        }
    }

    private fun fetchWeatherApiAlerts(farm: Farm): List<WeatherAlert> {
        return try {
            val response = webClient.get()
                .uri("$baseUrl/forecast.json") { uriBuilder ->
                    uriBuilder
                        .queryParam("key", apiKey)
                        .queryParam("q", "${farm.latitude},${farm.longitude}")
                        .queryParam("days", 1)
                        .queryParam("alerts", "yes")
                        .build()
                }
                .retrieve()
                .bodyToMono(WeatherApiForecastResponse::class.java)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
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
            logger.error(e) { "Error fetching alerts from WeatherAPI for farm: ${farm.name}" }
            emptyList()
        }
    }

    // OpenWeatherMap implementation
    private fun fetchOpenWeatherCurrent(farm: Farm): WeatherDataResponse? {
        return try {
            logger.info { "Fetching current weather from OpenWeatherMap for farm: ${farm.name}" }

            val response = webClient.get()
                .uri("https://api.openweathermap.org/data/2.5/weather") { uriBuilder ->
                    uriBuilder
                        .queryParam("lat", farm.latitude)
                        .queryParam("lon", farm.longitude)
                        .queryParam("appid", openWeatherApiKey)
                        .queryParam("units", "metric")
                        .build()
                }
                .retrieve()
                .bodyToMono(OpenWeatherCurrentResponse::class.java)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .block()

            response?.let { convertOpenWeatherCurrentToWeatherData(it, farm) }

        } catch (e: Exception) {
            logger.error(e) { "Error fetching current weather from OpenWeatherMap for farm: ${farm.name}" }
            null
        }
    }

    private fun fetchOpenWeatherForecast(farm: Farm, days: Int): List<WeatherDataResponse> {
        return try {
            logger.info { "Fetching ${days}-day forecast from OpenWeatherMap for farm: ${farm.name}" }

            val response = webClient.get()
                .uri("https://api.openweathermap.org/data/2.5/forecast") { uriBuilder ->
                    uriBuilder
                        .queryParam("lat", farm.latitude)
                        .queryParam("lon", farm.longitude)
                        .queryParam("appid", openWeatherApiKey)
                        .queryParam("units", "metric")
                        .queryParam("cnt", (days * 8).coerceAtMost(40)) // 8 forecasts per day, max 40
                        .build()
                }
                .retrieve()
                .bodyToMono(OpenWeatherForecastResponse::class.java)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .block()

            response?.let { convertOpenWeatherForecastToWeatherData(it, farm) } ?: emptyList()

        } catch (e: Exception) {
            logger.error(e) { "Error fetching forecast from OpenWeatherMap for farm: ${farm.name}" }
            emptyList()
        }
    }

    private fun fetchOpenWeatherHistorical(farm: Farm, date: LocalDate): WeatherDataResponse? {
        return try {
            val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond()

            val response = webClient.get()
                .uri("https://api.openweathermap.org/data/3.0/onecall/timemachine") { uriBuilder ->
                    uriBuilder
                        .queryParam("lat", farm.latitude)
                        .queryParam("lon", farm.longitude)
                        .queryParam("dt", timestamp)
                        .queryParam("appid", openWeatherApiKey)
                        .queryParam("units", "metric")
                        .build()
                }
                .retrieve()
                .bodyToMono(OpenWeatherHistoricalResponse::class.java)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .block()

            response?.data?.firstOrNull()?.let { data ->
                convertOpenWeatherHistoricalToWeatherData(data, farm, date)
            }

        } catch (e: Exception) {
            logger.error(e) { "Error fetching historical weather from OpenWeatherMap for farm: ${farm.name}" }
            null
        }
    }

    private fun fetchOpenWeatherAlerts(farm: Farm): List<WeatherAlert> {
        return try {
            val response = webClient.get()
                .uri("https://api.openweathermap.org/data/3.0/onecall") { uriBuilder ->
                    uriBuilder
                        .queryParam("lat", farm.latitude)
                        .queryParam("lon", farm.longitude)
                        .queryParam("appid", openWeatherApiKey)
                        .queryParam("exclude", "minutely,hourly,daily")
                        .build()
                }
                .retrieve()
                .bodyToMono(OpenWeatherOneCallResponse::class.java)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .block()

            response?.alerts?.map { alert ->
                WeatherAlert(
                    headline = alert.event,
                    description = alert.description,
                    severity = "Unknown", // OpenWeather doesn't provide severity
                    urgency = "Unknown",
                    areas = "Location: ${farm.latitude}, ${farm.longitude}",
                    effective = LocalDateTime.ofEpochSecond(alert.start, 0, ZoneId.systemDefault().rules.getOffset(LocalDateTime.now())).toString(),
                    expires = LocalDateTime.ofEpochSecond(alert.end, 0, ZoneId.systemDefault().rules.getOffset(LocalDateTime.now())).toString()
                )
            } ?: emptyList()

        } catch (e: Exception) {
            logger.error(e) { "Error fetching alerts from OpenWeatherMap for farm: ${farm.name}" }
            emptyList()
        }
    }

    // WeatherStack implementation (basic)
    private fun fetchWeatherStackCurrent(farm: Farm): WeatherDataResponse? {
        return try {
            logger.info { "Fetching current weather from WeatherStack for farm: ${farm.name}" }

            val response = webClient.get()
                .uri("http://api.weatherstack.com/current") { uriBuilder ->
                    uriBuilder
                        .queryParam("access_key", apiKey)
                        .queryParam("query", "${farm.latitude},${farm.longitude}")
                        .queryParam("units", "m")
                        .build()
                }
                .retrieve()
                .bodyToMono(WeatherStackCurrentResponse::class.java)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .block()

            response?.let { convertWeatherStackCurrentToWeatherData(it, farm) }

        } catch (e: Exception) {
            logger.error(e) { "Error fetching current weather from WeatherStack for farm: ${farm.name}" }
            null
        }
    }

    private fun fetchWeatherStackForecast(farm: Farm, days: Int): List<WeatherDataResponse> {
        // WeatherStack forecast requires paid plan
        logger.warn { "WeatherStack forecast requires paid subscription" }
        return emptyList()
    }

    private fun fetchWeatherStackHistorical(farm: Farm, date: LocalDate): WeatherDataResponse? {
        // WeatherStack historical requires paid plan
        logger.warn { "WeatherStack historical data requires paid subscription" }
        return null
    }

    // Conversion methods for WeatherAPI
    private fun convertWeatherApiCurrentToWeatherData(response: WeatherApiCurrentResponse, farm: Farm): WeatherDataResponse {
        return WeatherDataResponse(
            farmId = farm.id!!,
            date = LocalDate.now(),
            minTemperature = null,
            maxTemperature = null,
            averageTemperature = response.current.tempC?.let { BigDecimal(it.toString()) },
            rainfallMm = response.current.precipMm?.let { BigDecimal(it.toString()) },
            humidityPercentage = response.current.humidity?.let { BigDecimal(it.toString()) },
            windSpeedKmh = response.current.windKph?.let { BigDecimal(it.toString()) },
            pressure = response.current.pressureMb?.let { BigDecimal(it.toString()) },
            uvIndex = response.current.uv?.let { BigDecimal(it.toString()) },
            visibility = response.current.visKm?.let { BigDecimal(it.toString()) },
            cloudCover = response.current.cloud?.let { BigDecimal(it.toString()) },
            source = "WeatherAPI"
        )
    }

    private fun convertWeatherApiForecastToWeatherData(forecastDay: ForecastDay, farm: Farm): WeatherDataResponse {
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

    // Conversion methods for OpenWeatherMap
    private fun convertOpenWeatherCurrentToWeatherData(response: OpenWeatherCurrentResponse, farm: Farm): WeatherDataResponse {
        return WeatherDataResponse(
            farmId = farm.id!!,
            date = LocalDate.now(),
            minTemperature = response.main.tempMin?.let { BigDecimal(it.toString()) },
            maxTemperature = response.main.tempMax?.let { BigDecimal(it.toString()) },
            averageTemperature = response.main.temp?.let { BigDecimal(it.toString()) },
            rainfallMm = response.rain?.`1h`?.let { BigDecimal(it.toString()) },
            humidityPercentage = response.main.humidity?.let { BigDecimal(it.toString()) },
            windSpeedKmh = response.wind.speed?.let { BigDecimal((it * 3.6).toString()) }, // Convert m/s to km/h
            pressure = response.main.pressure?.let { BigDecimal(it.toString()) },
            visibility = response.visibility?.let { BigDecimal((it / 1000.0).toString()) }, // Convert m to km
            cloudCover = response.clouds.all?.let { BigDecimal(it.toString()) },
            source = "OpenWeatherMap"
        )
    }

    private fun convertOpenWeatherForecastToWeatherData(response: OpenWeatherForecastResponse, farm: Farm): List<WeatherDataResponse> {
        // Group by date and aggregate
        val dailyData = response.list.groupBy {
            LocalDateTime.ofEpochSecond(it.dt, 0, ZoneId.systemDefault().rules.getOffset(LocalDateTime.now())).toLocalDate()
        }

        return dailyData.map { (date, hourlyData) ->
            val temps = hourlyData.mapNotNull { it.main.temp }
            val rainfall = hourlyData.mapNotNull { it.rain?.`3h` }.sum()
            val humidityValues = hourlyData.mapNotNull { it.main.humidity }
            val windSpeed = hourlyData.mapNotNull { it.wind.speed }.maxOrNull() ?: 0.0

            WeatherDataResponse(
                farmId = farm.id!!,
                date = date,
                minTemperature = temps.minOrNull()?.let { BigDecimal(it.toString()) },
                maxTemperature = temps.maxOrNull()?.let { BigDecimal(it.toString()) },
                averageTemperature = if (temps.isNotEmpty()) BigDecimal(temps.average().toString()) else null,
                rainfallMm = if (rainfall > 0) BigDecimal(rainfall.toString()) else null,
                humidityPercentage = if (humidityValues.isNotEmpty()) BigDecimal(humidityValues.average().toString()) else null,
                windSpeedKmh = BigDecimal((windSpeed * 3.6).toString()),
                source = "OpenWeatherMap"
            )
        }
    }

    private fun convertOpenWeatherHistoricalToWeatherData(data: OpenWeatherHistoricalData, farm: Farm, date: LocalDate): WeatherDataResponse {
        return WeatherDataResponse(
            farmId = farm.id!!,
            date = date,
            minTemperature = null, // Historical data structure varies
            maxTemperature = null,
            averageTemperature = data.temp?.let { BigDecimal(it.toString()) },
            rainfallMm = data.rain?.`1h`?.let { BigDecimal(it.toString()) },
            humidityPercentage = data.humidity?.let { BigDecimal(it.toString()) },
            windSpeedKmh = data.windSpeed?.let { BigDecimal((it * 3.6).toString()) },
            pressure = data.pressure?.let { BigDecimal(it.toString()) },
            source = "OpenWeatherMap"
        )
    }

    // Conversion methods for WeatherStack
    private fun convertWeatherStackCurrentToWeatherData(response: WeatherStackCurrentResponse, farm: Farm): WeatherDataResponse {
        return WeatherDataResponse(
            farmId = farm.id!!,
            date = LocalDate.now(),
            minTemperature = null,
            maxTemperature = null,
            averageTemperature = response.current.temperature?.let { BigDecimal(it.toString()) },
            rainfallMm = response.current.precip?.let { BigDecimal(it.toString()) },
            humidityPercentage = response.current.humidity?.let { BigDecimal(it.toString()) },
            windSpeedKmh = response.current.windSpeed?.let { BigDecimal(it.toString()) },
            pressure = response.current.pressure?.let { BigDecimal(it.toString()) },
            visibility = response.current.visibility?.let { BigDecimal(it.toString()) },
            cloudCover = response.current.cloudcover?.let { BigDecimal(it.toString()) },
            source = "WeatherStack"
        )
    }
}

// Enhanced data classes for WeatherAPI.com
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

// OpenWeatherMap data classes
@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenWeatherCurrentResponse(
    val main: OpenWeatherMain,
    val wind: OpenWeatherWind,
    val clouds: OpenWeatherClouds,
    val rain: OpenWeatherRain?,
    val visibility: Int?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenWeatherMain(
    val temp: Double?,
    @JsonProperty("temp_min") val tempMin: Double?,
    @JsonProperty("temp_max") val tempMax: Double?,
    val pressure: Double?,
    val humidity: Int?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenWeatherWind(
    val speed: Double?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenWeatherClouds(
    val all: Int?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenWeatherRain(
    @JsonProperty("1h") val `1h`: Double?,
    @JsonProperty("3h") val `3h`: Double?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenWeatherForecastResponse(
    val list: List<OpenWeatherForecastItem>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenWeatherForecastItem(
    val dt: Long,
    val main: OpenWeatherMain,
    val wind: OpenWeatherWind,
    val rain: OpenWeatherRain?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenWeatherHistoricalResponse(
    val data: List<OpenWeatherHistoricalData>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenWeatherHistoricalData(
    val temp: Double?,
    val pressure: Double?,
    val humidity: Int?,
    @JsonProperty("wind_speed") val windSpeed: Double?,
    val rain: OpenWeatherRain?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenWeatherOneCallResponse(
    val alerts: List<OpenWeatherAlert>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenWeatherAlert(
    val event: String,
    val description: String,
    val start: Long,
    val end: Long
)

// WeatherStack data classes
@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherStackCurrentResponse(
    val current: WeatherStackCurrent
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherStackCurrent(
    val temperature: Int?,
    val precip: Double?,
    val humidity: Int?,
    @JsonProperty("wind_speed") val windSpeed: Int?,
    val pressure: Int?,
    val visibility: Int?,
    val cloudcover: Int?
)

// Weather alert data class (shared)
data class WeatherAlert(
    val headline: String,
    val description: String,
    val severity: String,
    val urgency: String,
    val areas: String,
    val effective: String,
    val expires: String
)