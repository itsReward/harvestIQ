/*
package com.maizeyield.service.impl

import com.maizeyield.dto.RecommendationResponse
import com.maizeyield.dto.YieldPredictionResponse
import com.maizeyield.exception.ResourceNotFoundException
import com.maizeyield.exception.UnauthorizedAccessException
import com.maizeyield.model.*
import com.maizeyield.repository.*
import com.maizeyield.service.FarmService
import com.maizeyield.service.external.AdvancedRecommendationService
import org.hibernate.validator.internal.util.Contracts.assertTrue
import org.postgresql.hostchooser.HostRequirement
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.*

class EnhancedRecommendationServiceImplTest {

    private lateinit var recommendationService: EnhancedRecommendationServiceImpl
    private lateinit var recommendationRepository: RecommendationRepository
    private lateinit var plantingSessionRepository: PlantingSessionRepository
    private lateinit var yieldPredictionRepository: YieldPredictionRepository
    private lateinit var yieldHistoryRepository: YieldHistoryRepository
    private lateinit var weatherDataRepository: WeatherDataRepository
    private lateinit var soilDataRepository: SoilDataRepository
    private lateinit var farmService: FarmService
    private lateinit var advancedRecommendationService: AdvancedRecommendationService

    @BeforeEach
    fun setUp() {
        recommendationRepository = mockk()
        plantingSessionRepository = mockk()
        yieldPredictionRepository = mockk()
        yieldHistoryRepository = mockk()
        weatherDataRepository = mockk()
        soilDataRepository = mockk()
        farmService = mockk()
        advancedRecommendationService = mockk()

        recommendationService = EnhancedRecommendationServiceImpl(
            recommendationRepository,
            plantingSessionRepository,
            yieldPredictionRepository,
            yieldHistoryRepository,
            weatherDataRepository,
            soilDataRepository,
            farmService,
            advancedRecommendationService
        )
    }

    @Test
    fun `should throw ResourceNotFoundException when planting session not found`() {
        // Given
        val userId = 1L
        val plantingSessionId = 1L
        val predictionResult = createMockPredictionResponse()

        every { plantingSessionRepository.findByIdOrNull(plantingSessionId) } returns null

        // When & Then
        assertThrows<ResourceNotFoundException> {
            recommendationService.analyzeAndGenerateRecommendations(userId, plantingSessionId, predictionResult)
        }
    }

    @Test
    fun `should throw UnauthorizedAccessException when user is not farm owner`() {
        // Given
        val userId = 1L
        val plantingSessionId = 1L
        val predictionResult = createMockPredictionResponse()
        val plantingSession = createMockPlantingSession()

        every { plantingSessionRepository.findByIdOrNull(plantingSessionId) } returns plantingSession
        every { farmService.isFarmOwner(userId, plantingSession.farm.id!!) } returns false

        // When & Then
        assertThrows<UnauthorizedAccessException> {
            recommendationService.analyzeAndGenerateRecommendations(userId, plantingSessionId, predictionResult)
        }
    }

    @Test
    fun `should generate critical recommendations when predicted yield is below critical threshold`() {
        // Given
        val userId = 1L
        val plantingSessionId = 1L
        val predictionResult = createMockPredictionResponse(predictedYield = 1.5) // Below 2.0 critical threshold
        val plantingSession = createMockPlantingSession()

        every { plantingSessionRepository.findByIdOrNull(plantingSessionId) } returns plantingSession
        every { farmService.isFarmOwner(userId, plantingSession.farm.id!!) } returns true
        every { yieldHistoryRepository.findByFarmAndMaizeVariety(HostRequirement.any(), HostRequirement.any()) } returns emptyList()
        every { weatherDataRepository.findByFarmAndDateBetween(HostRequirement.any(), HostRequirement.any(), any()) } returns emptyList()
        every { soilDataRepository.findLatestByFarm(HostRequirement.any()) } returns Optional.empty()
        every { recommendationRepository.save(HostRequirement.any()) } answers { firstArg<Recommendation>().apply { id = 1L } }

        // When
        val recommendations = recommendationService.analyzeAndGenerateRecommendations(userId, plantingSessionId, predictionResult)

        // Then
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.any { it.priority == "CRITICAL" })
        assertTrue(recommendations.any { it.category == "EMERGENCY" })
        verify { recommendationRepository.save(HostRequirement.any()) }
    }

    @Test
    fun `should generate yield optimization recommendations when yield deficit exceeds threshold`() {
        // Given
        val userId = 1L
        val plantingSessionId = 1L
        val predictionResult = createMockPredictionResponse(predictedYield = 4.0) // 33% below optimal 6.0
        val plantingSession = createMockPlantingSession()

        every { plantingSessionRepository.findByIdOrNull(plantingSessionId) } returns plantingSession
        every { farmService.isFarmOwner(userId, plantingSession.farm.id!!) } returns true
        every { yieldHistoryRepository.findByFarmAndMaizeVariety(HostRequirement.any(), HostRequirement.any()) } returns emptyList()
        every { weatherDataRepository.findByFarmAndDateBetween(HostRequirement.any(), HostRequirement.any(), any()) } returns emptyList()
        every { soilDataRepository.findLatestByFarm(HostRequirement.any()) } returns Optional.empty()
        every { recommendationRepository.save(HostRequirement.any()) } answers { firstArg<Recommendation>().apply { id = 1L } }

        // When
        val recommendations = recommendationService.analyzeAndGenerateRecommendations(userId, plantingSessionId, predictionResult)

        // Then
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.any { it.category == "YIELD_OPTIMIZATION" })
        verify { recommendationRepository.save(HostRequirement.any()) }
    }

    @Test
    fun `should generate drought stress recommendations when low rainfall detected`() {
        // Given
        val userId = 1L
        val plantingSessionId = 1L
        val predictionResult = createMockPredictionResponse(predictedYield = 4.0)
        val plantingSession = createMockPlantingSession()
        val lowRainfallWeather = listOf(
            createMockWeatherData(rainfall = 2.0), // Below 5.0 threshold
            createMockWeatherData(rainfall = 1.0)
        )

        every { plantingSessionRepository.findByIdOrNull(plantingSessionId) } returns plantingSession
        every { farmService.isFarmOwner(userId, plantingSession.farm.id!!) } returns true
        every { yieldHistoryRepository.findByFarmAndMaizeVariety(HostRequirement.any(), HostRequirement.any()) } returns emptyList()
        every { weatherDataRepository.findByFarmAndDateBetween(HostRequirement.any(), HostRequirement.any(), any()) } returns lowRainfallWeather
        every { soilDataRepository.findLatestByFarm(HostRequirement.any()) } returns Optional.empty()
        every { recommendationRepository.save(HostRequirement.any()) } answers { firstArg<Recommendation>().apply { id = 1L } }

        // When
        val recommendations = recommendationService.analyzeAndGenerateRecommendations(userId, plantingSessionId, predictionResult)

        // Then
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.any { it.category == "IRRIGATION" })
        assertTrue(recommendations.any { it.title.contains("Drought Stress") })
        verify { recommendationRepository.save(HostRequirement.any()) }
    }

    @Test
    fun `should generate fertilization recommendations when soil nutrients are deficient`() {
        // Given
        val userId = 1L
        val plantingSessionId = 1L
        val predictionResult = createMockPredictionResponse(predictedYield = 4.0)
        val plantingSession = createMockPlantingSession()
        val deficientSoil = createMockSoilData(nitrogen = 15.0, phosphorus = 10.0) // Below thresholds

        every { plantingSessionRepository.findByIdOrNull(plantingSessionId) } returns plantingSession
        every { farmService.isFarmOwner(userId, plantingSession.farm.id!!) } returns true
        every { yieldHistoryRepository.findByFarmAndMaizeVariety(HostRequirement.any(), HostRequirement.any()) } returns emptyList()
        every { weatherDataRepository.findByFarmAndDateBetween(HostRequirement.any(), HostRequirement.any(), any()) } returns emptyList()
        every { soilDataRepository.findLatestByFarm(HostRequirement.any()) } returns Optional.of(deficientSoil)
        every { recommendationRepository.save(HostRequirement.any()) } answers { firstArg<Recommendation>().apply { id = 1L } }

        // When
        val recommendations = recommendationService.analyzeAndGenerateRecommendations(userId, plantingSessionId, predictionResult)

        // Then
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.any { it.category == "FERTILIZATION" })
        assertTrue(recommendations.any { it.title.contains("Nitrogen") || it.title.contains("Phosphorus") })
        verify { recommendationRepository.save(HostRequirement.any()) }
    }

    @Test
    fun `should generate maintenance recommendations when yield is within acceptable range`() {
        // Given
        val userId = 1L
        val plantingSessionId = 1L
        val predictionResult = createMockPredictionResponse(predictedYield = 5.8, confidence = 85.0) // Good yield and confidence
        val plantingSession = createMockPlantingSession()

        every { plantingSessionRepository.findByIdOrNull(plantingSessionId) } returns plantingSession
        every { farmService.isFarmOwner(userId, plantingSession.farm.id!!) } returns true
        every { yieldHistoryRepository.findByFarmAndMaizeVariety(HostRequirement.any(), HostRequirement.any()) } returns emptyList()
        every { weatherDataRepository.findByFarmAndDateBetween(HostRequirement.any(), HostRequirement.any(), any()) } returns emptyList()
        every { soilDataRepository.findLatestByFarm(HostRequirement.any()) } returns Optional.empty()
        every { recommendationRepository.save(HostRequirement.any()) } answers { firstArg<Recommendation>().apply { id = 1L } }

        // When
        val recommendations = recommendationService.analyzeAndGenerateRecommendations(userId, plantingSessionId, predictionResult)

        // Then
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.any { it.category == "MAINTENANCE" })
        assertTrue(recommendations.any { it.priority == "LOW" })
        verify { recommendationRepository.save(HostRequirement.any()) }
    }

    @Test
    fun `should trigger advanced recommendations for critical situations`() {
        // Given
        val userId = 1L
        val plantingSessionId = 1L
        val predictionResult = createMockPredictionResponse(predictedYield = 1.0) // Very critical
        val plantingSession = createMockPlantingSession()
        val advancedRecommendations = listOf(createMockRecommendationResponse())

        every { plantingSessionRepository.findByIdOrNull(plantingSessionId) } returns plantingSession
        every { farmService.isFarmOwner(userId, plantingSession.farm.id!!) } returns true
        every { yieldHistoryRepository.findByFarmAndMaizeVariety(HostRequirement.any(), HostRequirement.any()) } returns emptyList()
        every { weatherDataRepository.findByFarmAndDateBetween(HostRequirement.any(), HostRequirement.any(), any()) } returns emptyList()
        every { soilDataRepository.findLatestByFarm(HostRequirement.any()) } returns Optional.empty()
        every { recommendationRepository.save(HostRequirement.any()) } answers { firstArg<Recommendation>().apply { id = 1L } }
        every { advancedRecommendationService.generateAdvancedRecommendations(HostRequirement.any(),
            HostRequirement.any(), any()) } returns advancedRecommendations

        // When
        val recommendations = recommendationService.analyzeAndGenerateRecommendations(userId, plantingSessionId, predictionResult)

        // Then
        assertTrue(recommendations.isNotEmpty())
        verify { advancedRecommendationService.generateAdvancedRecommendations(HostRequirement.any(),
            HostRequirement.any(), any()) }
    }

    @Test
    fun `should handle exception in advanced recommendations gracefully`() {
        // Given
        val userId = 1L
        val plantingSessionId = 1L
        val predictionResult = createMockPredictionResponse(predictedYield = 1.0)
        val plantingSession = createMockPlantingSession()

        every { plantingSessionRepository.findByIdOrNull(plantingSessionId) } returns plantingSession
        every { farmService.isFarmOwner(userId, plantingSession.farm.id!!) } returns true
        every { yieldHistoryRepository.findByFarmAndMaizeVariety(HostRequirement.any(), HostRequirement.any()) } returns emptyList()
        every { weatherDataRepository.findByFarmAndDateBetween(HostRequirement.any(), HostRequirement.any(), any()) } returns emptyList()
        every { soilDataRepository.findLatestByFarm(HostRequirement.any()) } returns Optional.empty()
        every { recommendationRepository.save(HostRequirement.any()) } answers { firstArg<Recommendation>().apply { id = 1L } }
        every { advancedRecommendationService.generateAdvancedRecommendations(HostRequirement.any(),
            HostRequirement.any(), any()) } throws Exception("API Error")

        // When
        val recommendations = recommendationService.analyzeAndGenerateRecommendations(userId, plantingSessionId, predictionResult)

        // Then
        // Should still generate basic recommendations despite advanced service failure
        assertTrue(recommendations.isNotEmpty())
        assertTrue(recommendations.any { it.priority == "CRITICAL" })
    }

    @Test
    fun `should calculate expected yield from historical data when available`() {
        // Given
        val userId = 1L
        val plantingSessionId = 1L
        val predictionResult = createMockPredictionResponse(predictedYield = 3.0)
        val plantingSession = createMockPlantingSession()
        val historicalYields = listOf(
            createMockYieldHistory(yield = 5.0),
            createMockYieldHistory(yield = 5.5),
            createMockYieldHistory(yield = 4.5)
        ) // Average = 5.0

        every { plantingSessionRepository.findByIdOrNull(plantingSessionId) } returns plantingSession
        every { farmService.isFarmOwner(userId, plantingSession.farm.id!!) } returns true
        every { yieldHistoryRepository.findByFarmAndMaizeVariety(HostRequirement.any(), HostRequirement.any()) } returns historicalYields
        every { weatherDataRepository.findByFarmAndDateBetween(HostRequirement.any(), HostRequirement.any(), any()) } returns emptyList()
        every { soilDataRepository.findLatestByFarm(HostRequirement.any()) } returns Optional.empty()
        every { recommendationRepository.save(HostRequirement.any()) } answers { firstArg<Recommendation>().apply { id = 1L } }

        // When
        val recommendations = recommendationService.analyzeAndGenerateRecommendations(userId, plantingSessionId, predictionResult)

        // Then
        // With historical average of 5.0 and prediction of 3.0, deficit is 40% (> 15% threshold)
        assertTrue(recommendations.any { it.category == "YIELD_OPTIMIZATION" })
    }

    // Helper methods for creating mock objects
    private fun createMockPredictionResponse(
        predictedYield: Double = 4.0,
        confidence: Double = 80.0
    ): YieldPredictionResponse {
        return YieldPredictionResponse(
            id = 1L,
            plantingSessionId = 1L,
            predictionDate = LocalDate.now(),
            predictedYieldTonsPerHectare = predictedYield,
            confidencePercentage = confidence,
            modelVersion = "v1.0",
            featuresUsed = listOf("weather", "soil", "variety")
        )
    }

    private fun createMockPlantingSession(): PlantingSession {
        val farm = Farm(
            id = 1L,
            name = "Test Farm",
            location = "Test Location",
            totalAreaHectares = 10.0,
            user = User(),
            soilData = mutableListOf(),
            weatherData = mutableListOf(),
            plantingSessions = mutableListOf()
        )

        val variety = MaizeVariety(
            id = 1L,
            name = "Test Variety",
            description = "Test Description",
            maturityDays = 120,
            averageYieldTonsPerHectare = 5.0,
            droughtResistant = false,
            pestResistant = false,
            optimalTempMin = 18.0,
            optimalTempMax = 30.0,
            optimalRainfallMm = 600.0
        )

        return PlantingSession(
            id = 1L,
            farm = farm,
            maizeVariety = variety,
            plantingDate = LocalDate.now().minusDays(30),
            expectedHarvestDate = LocalDate.now().plusDays(90),
            areaPlantedHectares = 5.0,
            seedsPerHectare = 25000
        )
    }

    private fun createMockWeatherData(
        rainfall: Double = 10.0,
        maxTemp: Double = 25.0
    ): WeatherData {
        return WeatherData(
            id = 1L,
            farm = Farm(),
            date = LocalDate.now(),
            minTemp = 15.0,
            maxTemp = maxTemp,
            avgTemp = 20.0,
            rainfall = rainfall,
            humidity = 70.0,
            windSpeed = 5.0,
            source = "test"
        )
    }

    private fun createMockSoilData(
        nitrogen: Double = 25.0,
        phosphorus: Double = 20.0
    ): SoilData {
        return SoilData(
            id = 1L,
            farm = Farm(),
            sampleDate = LocalDate.now(),
            soilType = "Clay",
            phLevel = 6.5,
            organicMatter = 3.0,
            nitrogen = nitrogen,
            phosphorus = phosphorus,
            potassium = 200.0,
            moisture = 35.0
        )
    }

    private fun createMockYieldHistory(yield: Double): YieldHistory {
        return YieldHistory(
            id = 1L,
            farm = Farm(),
            maizeVariety = MaizeVariety(),
            plantingSession = null,
            harvestDate = LocalDate.now(),
            yieldTonsPerHectare = yield,
            qualityRating = "Good"
        )
    }

    private fun createMockRecommendationResponse(): RecommendationResponse {
        return RecommendationResponse(
            id = 1L,
            plantingSessionId = 1L,
            category = "IRRIGATION",
            title = "Test Recommendation",
            description = "Test Description",
            priority = "HIGH",
            recommendationDate = LocalDate.now(),
            isViewed = false,
            isImplemented = false,
            confidence = 85.0f
        )
    }
}*/
