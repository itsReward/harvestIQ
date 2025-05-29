package com.maizeyield.service.external

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.maizeyield.dto.RecommendationResponse
import com.maizeyield.exception.DataIntegrityException
import com.maizeyield.model.PlantingSession
import com.maizeyield.model.Recommendation
import com.maizeyield.model.SoilData
import com.maizeyield.model.WeatherData
import com.maizeyield.repository.RecommendationRepository
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class AdvancedRecommendationService(
    private val webClient: WebClient,
    private val recommendationRepository: RecommendationRepository,
    @Value("\${recommendation.api.key:}") private val apiKey: String,
    @Value("\${recommendation.api.base-url:}") private val baseUrl: String,
    @Value("\${recommendation.api.enabled:false}") private val apiEnabled: Boolean
) {

    /**
     * Generate advanced AI-powered recommendations using external service
     */
    fun generateAdvancedRecommendations(
        plantingSession: PlantingSession,
        recentWeatherData: List<WeatherData>,
        latestSoilData: SoilData?
    ): List<RecommendationResponse> {

        return if (apiEnabled && baseUrl.isNotBlank()) {
            try {
                generateExternalRecommendations(plantingSession, recentWeatherData, latestSoilData)
            } catch (e: Exception) {
                logger.warn(e) { "External recommendation service failed, falling back to local recommendations" }
                generateLocalAdvancedRecommendations(plantingSession, recentWeatherData, latestSoilData)
            }
        } else {
            logger.info { "External recommendation service disabled, using local advanced recommendations" }
            generateLocalAdvancedRecommendations(plantingSession, recentWeatherData, latestSoilData)
        }
    }

    private fun generateExternalRecommendations(
        plantingSession: PlantingSession,
        recentWeatherData: List<WeatherData>,
        latestSoilData: SoilData?
    ): List<RecommendationResponse> {

        logger.info { "Generating advanced recommendations using external AI service for session: ${plantingSession.id}" }

        val request = buildRecommendationRequest(plantingSession, recentWeatherData, latestSoilData)

        val response = webClient.post()
            .uri("$baseUrl/recommendations/generate")
            .header("Authorization", "Bearer $apiKey")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ExternalRecommendationResponse::class.java)
            .block()

        return response?.recommendations?.map { extRec ->
            val recommendation = Recommendation(
                plantingSession = plantingSession,
                recommendationDate = LocalDate.now(),
                category = extRec.category,
                title = extRec.title,
                description = extRec.description,
                priority = extRec.priority,
                confidence = extRec.confidence
            )

            val savedRecommendation = recommendationRepository.save(recommendation)
            mapToRecommendationResponse(savedRecommendation)
        } ?: emptyList()
    }

    private fun generateLocalAdvancedRecommendations(
        plantingSession: PlantingSession,
        recentWeatherData: List<WeatherData>,
        latestSoilData: SoilData?
    ): List<RecommendationResponse> {

        logger.info { "Generating advanced local recommendations for session: ${plantingSession.id}" }

        val recommendations = mutableListOf<Recommendation>()
        val currentDate = LocalDate.now()
        val daysSincePlanting = currentDate.toEpochDay() - plantingSession.plantingDate.toEpochDay()

        // Advanced weather-based recommendations
        if (recentWeatherData.isNotEmpty()) {
            recommendations.addAll(generateWeatherBasedRecommendations(plantingSession, recentWeatherData, daysSincePlanting.toInt()))
        }

        // Advanced soil-based recommendations
        latestSoilData?.let { soilData ->
            recommendations.addAll(generateSoilBasedRecommendations(plantingSession, soilData, daysSincePlanting.toInt()))
        }

        // Growth stage specific advanced recommendations
        recommendations.addAll(generateGrowthStageRecommendations(plantingSession, daysSincePlanting.toInt()))

        // Variety-specific recommendations
        recommendations.addAll(generateVarietySpecificRecommendations(plantingSession, recentWeatherData, daysSincePlanting.toInt()))

        // Save all recommendations
        val savedRecommendations = if (recommendations.isNotEmpty()) {
            recommendationRepository.saveAll(recommendations)
        } else {
            emptyList()
        }

        return savedRecommendations.map { mapToRecommendationResponse(it) }
    }

    private fun generateWeatherBasedRecommendations(
        plantingSession: PlantingSession,
        weatherData: List<WeatherData>,
        daysSincePlanting: Int
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        val currentDate = LocalDate.now()

        // Calculate weather averages
        val avgTemp = weatherData.mapNotNull { it.averageTemperature?.toDouble() }.average()
        val totalRainfall = weatherData.mapNotNull { it.rainfallMm?.toDouble() }.sum()
        val avgHumidity = weatherData.mapNotNull { it.humidityPercentage?.toDouble() }.average()
        val maxWindSpeed = weatherData.mapNotNull { it.windSpeedKmh?.toDouble() }.maxOrNull() ?: 0.0

        // Advanced temperature recommendations
        when {
            !avgTemp.isNaN() && avgTemp > 35.0 && daysSincePlanting in 30..80 -> {
                recommendations.add(
                    Recommendation(
                        plantingSession = plantingSession,
                        recommendationDate = currentDate,
                        category = "HEAT_STRESS",
                        title = "Critical Heat Stress Management",
                        description = "Extreme heat (${String.format("%.1f", avgTemp)}°C) during reproductive phase. Apply mulch, increase irrigation frequency to 2-3 times daily, and consider shade netting for critical areas.",
                        priority = "CRITICAL",
                        confidence = 0.95f
                    )
                )
            }
            !avgTemp.isNaN() && avgTemp < 15.0 && daysSincePlanting < 30 -> {
                recommendations.add(
                    Recommendation(
                        plantingSession = plantingSession,
                        recommendationDate = currentDate,
                        category = "COLD_PROTECTION",
                        title = "Cold Weather Protection",
                        description = "Low temperatures (${String.format("%.1f", avgTemp)}°C) may slow germination and early growth. Consider row covers or plastic tunnels for protection.",
                        priority = "HIGH",
                        confidence = 0.88f
                    )
                )
            }
        }

        // Advanced rainfall recommendations
        when {
            totalRainfall < 10.0 && daysSincePlanting in 40..100 -> {
                recommendations.add(
                    Recommendation(
                        plantingSession = plantingSession,
                        recommendationDate = currentDate,
                        category = "DROUGHT_MANAGEMENT",
                        title = "Drought Stress Mitigation",
                        description = "Severe rainfall deficit (${String.format("%.1f", totalRainfall)}mm in 7 days). Implement deficit irrigation strategy and apply organic mulch to conserve soil moisture.",
                        priority = "CRITICAL",
                        confidence = 0.92f
                    )
                )
            }
            totalRainfall > 100.0 -> {
                recommendations.add(
                    Recommendation(
                        plantingSession = plantingSession,
                        recommendationDate = currentDate,
                        category = "WATERLOG_PREVENTION",
                        title = "Waterlogging Prevention",
                        description = "Excessive rainfall (${String.format("%.1f", totalRainfall)}mm). Ensure drainage channels are clear and consider fungicide application to prevent root rot.",
                        priority = "HIGH",
                        confidence = 0.87f
                    )
                )
            }
        }

        // Humidity-based disease risk recommendations
        if (!avgHumidity.isNaN() && avgHumidity > 80.0 && !avgTemp.isNaN() && avgTemp > 25.0) {
            recommendations.add(
                Recommendation(
                    plantingSession = plantingSession,
                    recommendationDate = currentDate,
                    category = "DISEASE_PREVENTION",
                    title = "High Disease Risk Alert",
                    description = "High humidity (${String.format("%.1f", avgHumidity)}%) and warm temperatures create ideal conditions for fungal diseases. Apply preventive fungicide and improve air circulation.",
                    priority = "HIGH",
                    confidence = 0.85f
                )
            )
        }

        // Wind damage recommendations
        if (maxWindSpeed > 50.0) {
            recommendations.add(
                Recommendation(
                    plantingSession = plantingSession,
                    recommendationDate = currentDate,
                    category = "WIND_PROTECTION",
                    title = "Wind Damage Prevention",
                    description = "Strong winds (${String.format("%.1f", maxWindSpeed)} km/h) may cause lodging. Consider staking tall plants and check for mechanical damage.",
                    priority = "MEDIUM",
                    confidence = 0.78f
                )
            )
        }

        return recommendations
    }

    private fun generateSoilBasedRecommendations(
        plantingSession: PlantingSession,
        soilData: SoilData,
        daysSincePlanting: Int
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        val currentDate = LocalDate.now()

        // Advanced nutrient management
        soilData.nitrogenContent?.let { nitrogen ->
            when {
                nitrogen.toDouble() < 1.0 && daysSincePlanting in 20..60 -> {
                    recommendations.add(
                        Recommendation(
                            plantingSession = plantingSession,
                            recommendationDate = currentDate,
                            category = "NUTRIENT_MANAGEMENT",
                            title = "Critical Nitrogen Deficiency",
                            description = "Severe nitrogen deficiency (${nitrogen}%). Apply immediate foliar nitrogen (urea 2%) and side-dress with 150kg/ha of CAN for rapid uptake.",
                            priority = "CRITICAL",
                            confidence = 0.93f
                        )
                    )
                }
                nitrogen.toDouble() in 1.0..1.5 && daysSincePlanting in 30..70 -> {
                    recommendations.add(
                        Recommendation(
                            plantingSession = plantingSession,
                            recommendationDate = currentDate,
                            category = "NUTRIENT_MANAGEMENT",
                            title = "Optimize Nitrogen Supply",
                            description = "Moderate nitrogen levels (${nitrogen}%). Apply split application of nitrogen fertilizer - 100kg/ha now and 50kg/ha at tasseling stage.",
                            priority = "HIGH",
                            confidence = 0.87f
                        )
                    )
                }
                else -> {
                    throw DataIntegrityException("nitrogen levels are invalid")
                }
            }
        }

        // Advanced pH management
        soilData.phLevel?.let { ph ->
            when {
                ph.toDouble() < 5.5 -> {
                    recommendations.add(
                        Recommendation(
                            plantingSession = plantingSession,
                            recommendationDate = currentDate,
                            category = "SOIL_CHEMISTRY",
                            title = "Soil Acidification Treatment",
                            description = "Severe soil acidity (pH ${ph}) is limiting nutrient availability. Apply agricultural lime at 3-4 tonnes/ha and consider foliar micronutrient application.",
                            priority = "HIGH",
                            confidence = 0.91f
                        )
                    )
                }
                ph.toDouble() > 8.0 -> {
                    recommendations.add(
                        Recommendation(
                            plantingSession = plantingSession,
                            recommendationDate = currentDate,
                            category = "SOIL_CHEMISTRY",
                            title = "Alkaline Soil Management",
                            description = "High soil pH (${ph}) may cause micronutrient deficiencies. Apply sulfur at 200kg/ha and use acidifying fertilizers like ammonium sulfate.",
                            priority = "MEDIUM",
                            confidence = 0.84f
                        )
                    )
                }

                else -> {
                    throw DataIntegrityException("ph numbers  not valid")
                }
            }
        }

        // Soil moisture management
        soilData.moistureContent?.let { moisture ->
            when {
                moisture.toDouble() < 20.0 && daysSincePlanting in 40..80 -> {
                    recommendations.add(
                        Recommendation(
                            plantingSession = plantingSession,
                            recommendationDate = currentDate,
                            category = "IRRIGATION",
                            title = "Critical Soil Moisture Deficit",
                            description = "Low soil moisture (${moisture}%) during critical growth period. Increase irrigation to 25-30mm per week and apply mulch to reduce evaporation.",
                            priority = "CRITICAL",
                            confidence = 0.89f
                        )
                    )
                }
                moisture.toDouble() > 80.0 -> {
                    recommendations.add(
                        Recommendation(
                            plantingSession = plantingSession,
                            recommendationDate = currentDate,
                            category = "DRAINAGE",
                            title = "Excess Soil Moisture Management",
                            description = "High soil moisture (${moisture}%) may cause root problems. Improve drainage and reduce irrigation frequency to prevent waterlogging.",
                            priority = "MEDIUM",
                            confidence = 0.82f
                        )
                    )
                }

                else -> {
                    throw DataIntegrityException("moisture levels are invalid")
                }
            }
        }

        return recommendations
    }

    private fun generateGrowthStageRecommendations(
        plantingSession: PlantingSession,
        daysSincePlanting: Int
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        val currentDate = LocalDate.now()

        // Advanced growth stage recommendations
        when (daysSincePlanting) {
            in 5..10 -> {
                recommendations.add(
                    Recommendation(
                        plantingSession = plantingSession,
                        recommendationDate = currentDate,
                        category = "EMERGENCE",
                        title = "Emergence Stage Monitoring",
                        description = "Critical emergence period. Monitor for uniform germination, check soil crusting, and ensure adequate soil moisture. Apply starter fertilizer if not done at planting.",
                        priority = "HIGH",
                        confidence = 0.90f
                    )
                )
            }
            in 25..35 -> {
                recommendations.add(
                    Recommendation(
                        plantingSession = plantingSession,
                        recommendationDate = currentDate,
                        category = "WEED_CONTROL",
                        title = "Critical Weed Control Period",
                        description = "Entering critical weed-free period. Apply post-emergence herbicide or conduct mechanical weeding. Weeds competing now will significantly impact yield.",
                        priority = "CRITICAL",
                        confidence = 0.95f
                    )
                )
            }
            in 45..55 -> {
                recommendations.add(
                    Recommendation(
                        plantingSession = plantingSession,
                        recommendationDate = currentDate,
                        category = "NUTRIENT_TIMING",
                        title = "Pre-Tasseling Nutrition Boost",
                        description = "Rapid growth phase before tasseling. Apply side-dress nitrogen and ensure adequate potassium levels. This is critical for ear development.",
                        priority = "HIGH",
                        confidence = 0.88f
                    )
                )
            }
            in 65..75 -> {
                recommendations.add(
                    Recommendation(
                        plantingSession = plantingSession,
                        recommendationDate = currentDate,
                        category = "REPRODUCTIVE_SUPPORT",
                        title = "Pollination Period Support",
                        description = "Tasseling and silking stage. Ensure consistent moisture (no stress), monitor for silk clipping by insects, and avoid any field operations that may damage pollen.",
                        priority = "CRITICAL",
                        confidence = 0.93f
                    )
                )
            }
            in 90..110 -> {
                recommendations.add(
                    Recommendation(
                        plantingSession = plantingSession,
                        recommendationDate = currentDate,
                        category = "GRAIN_FILLING",
                        title = "Grain Filling Optimization",
                        description = "Grain filling period. Maintain consistent moisture, monitor for late-season diseases, and avoid any plant stress that could reduce kernel weight.",
                        priority = "HIGH",
                        confidence = 0.87f
                    )
                )
            }
        }

        return recommendations
    }

    private fun generateVarietySpecificRecommendations(
        plantingSession: PlantingSession,
        weatherData: List<WeatherData>,
        daysSincePlanting: Int
    ): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        val currentDate = LocalDate.now()
        val variety = plantingSession.maizeVariety

        // Drought-resistant variety specific recommendations
        if (variety.droughtResistance && weatherData.isNotEmpty()) {
            val totalRainfall = weatherData.mapNotNull { it.rainfallMm?.toDouble() }.sum()
            if (totalRainfall < 15.0) {
                recommendations.add(
                    Recommendation(
                        plantingSession = plantingSession,
                        recommendationDate = currentDate,
                        category = "VARIETY_ADVANTAGE",
                        title = "Leverage Drought Tolerance",
                        description = "Your drought-resistant variety ${variety.name} can handle current dry conditions better than conventional varieties. Maintain minimal irrigation and avoid overwatering.",
                        priority = "LOW",
                        confidence = 0.82f
                    )
                )
            }
        }

        // Early maturing variety recommendations
        if (variety.maturityDays < 100 && daysSincePlanting > 70) {
            recommendations.add(
                Recommendation(
                    plantingSession = plantingSession,
                    recommendationDate = currentDate,
                    category = "HARVEST_TIMING",
                    title = "Early Variety Harvest Preparation",
                    description = "Your early-maturing variety ${variety.name} is approaching harvest. Begin monitoring grain moisture content and prepare harvesting equipment.",
                    priority = "MEDIUM",
                    confidence = 0.85f
                )
            )
        }

        return recommendations
    }

    private fun buildRecommendationRequest(
        plantingSession: PlantingSession,
        recentWeatherData: List<WeatherData>,
        latestSoilData: SoilData?
    ): ExternalRecommendationRequest {
        val currentDate = LocalDate.now()
        val daysSincePlanting = currentDate.toEpochDay() - plantingSession.plantingDate.toEpochDay()

        return ExternalRecommendationRequest(
            farmId = plantingSession.farm.id!!,
            plantingSessionId = plantingSession.id!!,
            cropType = "MAIZE",
            variety = plantingSession.maizeVariety.name,
            plantingDate = plantingSession.plantingDate,
            daysSincePlanting = daysSincePlanting.toInt(),
            farmLocation = FarmLocation(
                latitude = plantingSession.farm.latitude?.toDouble(),
                longitude = plantingSession.farm.longitude?.toDouble(),
                elevation = plantingSession.farm.elevation?.toDouble()
            ),
            soilData = latestSoilData?.let { soil ->
                SoilDataRequest(
                    soilType = soil.soilType,
                    phLevel = soil.phLevel?.toDouble(),
                    organicMatter = soil.organicMatterPercentage?.toDouble(),
                    nitrogen = soil.nitrogenContent?.toDouble(),
                    phosphorus = soil.phosphorusContent?.toDouble(),
                    potassium = soil.potassiumContent?.toDouble(),
                    moisture = soil.moistureContent?.toDouble()
                )
            },
            weatherData = recentWeatherData.map { weather ->
                WeatherDataRequest(
                    date = weather.date,
                    minTemp = weather.minTemperature?.toDouble(),
                    maxTemp = weather.maxTemperature?.toDouble(),
                    avgTemp = weather.averageTemperature?.toDouble(),
                    rainfall = weather.rainfallMm?.toDouble(),
                    humidity = weather.humidityPercentage?.toDouble(),
                    windSpeed = weather.windSpeedKmh?.toDouble()
                )
            },
            varietyInfo = VarietyInfo(
                maturityDays = plantingSession.maizeVariety.maturityDays,
                droughtResistant = plantingSession.maizeVariety.droughtResistance,
                optimalTempMin = plantingSession.maizeVariety.optimalTemperatureMin?.toDouble(),
                optimalTempMax = plantingSession.maizeVariety.optimalTemperatureMax?.toDouble()
            )
        )
    }

    private fun mapToRecommendationResponse(recommendation: Recommendation): RecommendationResponse {
        return RecommendationResponse(
            id = recommendation.id!!,
            plantingSessionId = recommendation.plantingSession.id!!,
            category = recommendation.category,
            title = recommendation.title,
            description = recommendation.description,
            priority = recommendation.priority,
            recommendationDate = recommendation.recommendationDate,
            isViewed = recommendation.isViewed,
            isImplemented = recommendation.isImplemented,
            confidence = recommendation.confidence
        )
    }
}

// Data classes for external recommendation service
data class ExternalRecommendationRequest(
    val farmId: Long,
    val plantingSessionId: Long,
    val cropType: String,
    val variety: String,
    val plantingDate: LocalDate,
    val daysSincePlanting: Int,
    val farmLocation: FarmLocation,
    val soilData: SoilDataRequest?,
    val weatherData: List<WeatherDataRequest>,
    val varietyInfo: VarietyInfo
)

data class FarmLocation(
    val latitude: Double?,
    val longitude: Double?,
    val elevation: Double?
)

data class SoilDataRequest(
    val soilType: String,
    val phLevel: Double?,
    val organicMatter: Double?,
    val nitrogen: Double?,
    val phosphorus: Double?,
    val potassium: Double?,
    val moisture: Double?
)

data class WeatherDataRequest(
    val date: LocalDate,
    val minTemp: Double?,
    val maxTemp: Double?,
    val avgTemp: Double?,
    val rainfall: Double?,
    val humidity: Double?,
    val windSpeed: Double?
)

data class VarietyInfo(
    val maturityDays: Int,
    val droughtResistant: Boolean,
    val optimalTempMin: Double?,
    val optimalTempMax: Double?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalRecommendationResponse(
    val recommendations: List<ExternalRecommendation>,
    val confidence: Float,
    val model: String,
    val timestamp: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalRecommendation(
    val category: String,
    val title: String,
    val description: String,
    val priority: String,
    val confidence: Float,
    val reasoning: String?,
    val actionItems: List<String>?,
    val expectedOutcome: String?
)