package com.maizeyield.controller

import com.maizeyield.dto.RecommendationResponse
import com.maizeyield.dto.YieldPredictionResponse
import com.maizeyield.service.RecommendationService
import com.maizeyield.service.impl.PredictionRecommendationIntegrationService
import com.maizeyield.service.impl.RecommendationAnalysisResult
import com.maizeyield.service.impl.BatchPredictionItem
import com.maizeyield.service.impl.BatchRecommendationResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendation Management", description = "APIs for managing farming recommendations based on yield predictions")
@CrossOrigin(origins = ["*"], maxAge = 3600)
class RecommendationController(
    private val recommendationService: RecommendationService,
    private val integrationService: PredictionRecommendationIntegrationService
) {

    // ========================================
    // CORE RECOMMENDATION ENDPOINTS
    // ========================================

    @PostMapping("/analyze/{userId}/{plantingSessionId}")
    @Operation(
        summary = "Analyze prediction and generate recommendations",
        description = "Analyzes yield prediction results and generates contextual recommendations when yield is below optimum. " +
                "This is the main endpoint that triggers recommendation generation based on prediction analysis."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Recommendations generated successfully"),
            ApiResponse(responseCode = "400", description = "Invalid prediction data or parameters"),
            ApiResponse(responseCode = "404", description = "Planting session not found"),
            ApiResponse(responseCode = "500", description = "Internal server error during analysis")
        ]
    )
    fun analyzeAndGenerateRecommendations(
        @Parameter(description = "User ID of the farm owner", example = "1")
        @PathVariable userId: Long,
        @Parameter(description = "Planting session ID to analyze", example = "123")
        @PathVariable plantingSessionId: Long,
        @Parameter(description = "Yield prediction result to analyze")
        @Valid @RequestBody predictionResult: YieldPredictionResponse
    ): ResponseEntity<RecommendationAnalysisResult> {
        logger.info { "Analyzing prediction for user $userId, session $plantingSessionId" }

        return try {
            val result = integrationService.triggerRecommendationAnalysis(
                userId,
                plantingSessionId,
                predictionResult
            )

            logger.info { "Analysis completed: ${result.totalRecommendations} recommendations generated" }
            ResponseEntity.ok(result)

        } catch (e: Exception) {
            logger.error(e) { "Failed to analyze prediction for session $plantingSessionId" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                    RecommendationAnalysisResult(
                        success = false,
                        errorMessage = e.message ?: "Unknown error occurred during analysis"
                    )
                )
        }
    }

    @PostMapping("/batch-analyze/{userId}")
    @Operation(
        summary = "Batch process prediction recommendations",
        description = "Processes multiple predictions and generates recommendations in batch. " +
                "Useful for analyzing multiple planting sessions simultaneously."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Batch processing completed"),
            ApiResponse(responseCode = "400", description = "Invalid batch data"),
            ApiResponse(responseCode = "403", description = "Unauthorized batch request")
        ]
    )
    fun processBatchRecommendations(
        @Parameter(description = "User ID initiating batch processing")
        @PathVariable userId: Long,
        @Parameter(description = "List of predictions to process")
        @Valid @RequestBody predictions: List<BatchPredictionItem>
    ): ResponseEntity<BatchRecommendationResult> {
        logger.info { "Processing batch recommendations for user $userId, ${predictions.size} items" }

        // Validate that all predictions belong to the user
        val unauthorizedItems = predictions.filter { it.userId != userId }
        if (unauthorizedItems.isNotEmpty()) {
            logger.warn { "Unauthorized batch request: user $userId attempted to process items for other users" }
            return ResponseEntity.badRequest().build()
        }

        val result = integrationService.processBatchPredictionRecommendations(predictions)
        logger.info { "Batch processing completed: ${result.successCount}/${result.totalProcessed} successful" }

        return ResponseEntity.ok(result)
    }

    // ========================================
    // RECOMMENDATION RETRIEVAL ENDPOINTS
    // ========================================

    @GetMapping("/{userId}/planting-session/{plantingSessionId}")
    @Operation(
        summary = "Get all recommendations for planting session",
        description = "Retrieves all recommendations for a specific planting session, ordered by date (newest first)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully"),
            ApiResponse(responseCode = "404", description = "Planting session not found"),
            ApiResponse(responseCode = "403", description = "Access denied to planting session")
        ]
    )
    fun getRecommendationsByPlantingSession(
        @Parameter(description = "User ID of the farm owner")
        @PathVariable userId: Long,
        @Parameter(description = "Planting session ID")
        @PathVariable plantingSessionId: Long
    ): ResponseEntity<List<RecommendationResponse>> {
        logger.debug { "Retrieving recommendations for user $userId, session $plantingSessionId" }

        val recommendations = recommendationService.getRecommendationsByPlantingSession(
            userId,
            plantingSessionId
        )

        logger.debug { "Found ${recommendations.size} recommendations for session $plantingSessionId" }
        return ResponseEntity.ok(recommendations)
    }

    @GetMapping("/{userId}/planting-session/{plantingSessionId}/category/{category}")
    @Operation(
        summary = "Get recommendations by category",
        description = "Retrieves recommendations filtered by category (e.g., IRRIGATION, FERTILIZATION, PEST_CONTROL)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully"),
            ApiResponse(responseCode = "404", description = "Planting session not found"),
            ApiResponse(responseCode = "400", description = "Invalid category")
        ]
    )
    fun getRecommendationsByCategory(
        @Parameter(description = "User ID of the farm owner")
        @PathVariable userId: Long,
        @Parameter(description = "Planting session ID")
        @PathVariable plantingSessionId: Long,
        @Parameter(
            description = "Recommendation category",
            example = "IRRIGATION",
            schema = io.swagger.v3.oas.annotations.media.Schema(
                allowableValues = ["EMERGENCY", "YIELD_OPTIMIZATION", "IRRIGATION", "FERTILIZATION",
                    "PEST_CONTROL", "DISEASE_MANAGEMENT", "SOIL_MANAGEMENT",
                    "CROP_PROTECTION", "DRAINAGE", "DATA_QUALITY", "MONITORING", "MAINTENANCE"]
            )
        )
        @PathVariable category: String
    ): ResponseEntity<List<RecommendationResponse>> {
        logger.debug { "Retrieving $category recommendations for user $userId, session $plantingSessionId" }

        val recommendations = recommendationService.getRecommendationsByCategory(
            userId,
            plantingSessionId,
            category.uppercase()
        )

        return ResponseEntity.ok(recommendations)
    }

    @GetMapping("/{userId}/planting-session/{plantingSessionId}/priority/{priority}")
    @Operation(
        summary = "Get recommendations by priority",
        description = "Retrieves recommendations filtered by priority level (CRITICAL, HIGH, MEDIUM, LOW)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully"),
            ApiResponse(responseCode = "404", description = "Planting session not found"),
            ApiResponse(responseCode = "400", description = "Invalid priority level")
        ]
    )
    fun getRecommendationsByPriority(
        @Parameter(description = "User ID of the farm owner")
        @PathVariable userId: Long,
        @Parameter(description = "Planting session ID")
        @PathVariable plantingSessionId: Long,
        @Parameter(
            description = "Priority level",
            example = "HIGH",
            schema = io.swagger.v3.oas.annotations.media.Schema(
                allowableValues = ["CRITICAL", "HIGH", "MEDIUM", "LOW"]
            )
        )
        @PathVariable priority: String
    ): ResponseEntity<List<RecommendationResponse>> {
        logger.debug { "Retrieving $priority priority recommendations for user $userId, session $plantingSessionId" }

        val recommendations = recommendationService.getRecommendationsByPriority(
            userId,
            plantingSessionId,
            priority.uppercase()
        )

        return ResponseEntity.ok(recommendations)
    }

    // ========================================
    // RECOMMENDATION GENERATION ENDPOINTS
    // ========================================

    @PostMapping("/{userId}/generate/{plantingSessionId}")
    @Operation(
        summary = "Generate general recommendations",
        description = "Generates general recommendations for a planting session based on current conditions. " +
                "This endpoint creates recommendations when no specific prediction analysis is available."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Recommendations generated successfully"),
            ApiResponse(responseCode = "404", description = "Planting session not found"),
            ApiResponse(responseCode = "500", description = "Failed to generate recommendations")
        ]
    )
    fun generateRecommendations(
        @Parameter(description = "User ID of the farm owner")
        @PathVariable userId: Long,
        @Parameter(description = "Planting session ID")
        @PathVariable plantingSessionId: Long
    ): ResponseEntity<List<RecommendationResponse>> {
        logger.info { "Generating general recommendations for user $userId, session $plantingSessionId" }

        return try {
            val recommendations = recommendationService.generateRecommendations(
                userId,
                plantingSessionId
            )

            logger.info { "Generated ${recommendations.size} general recommendations for session $plantingSessionId" }
            ResponseEntity.ok(recommendations)

        } catch (e: Exception) {
            logger.error(e) { "Failed to generate recommendations for session $plantingSessionId" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    @PostMapping("/emergency-analysis/{userId}/{plantingSessionId}")
    @Operation(
        summary = "Emergency recommendation analysis",
        description = "Triggers emergency analysis when critical yield thresholds are detected. " +
                "Provides immediate actionable recommendations for urgent situations."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Emergency analysis completed"),
            ApiResponse(responseCode = "404", description = "Planting session not found"),
            ApiResponse(responseCode = "400", description = "Invalid prediction data")
        ]
    )
    fun emergencyAnalysis(
        @Parameter(description = "User ID of the farm owner")
        @PathVariable userId: Long,
        @Parameter(description = "Planting session ID")
        @PathVariable plantingSessionId: Long,
        @Parameter(description = "Critical prediction result triggering emergency analysis")
        @Valid @RequestBody predictionResult: YieldPredictionResponse
    ): ResponseEntity<EmergencyRecommendationResponse> {
        logger.warn { "Emergency analysis triggered for user $userId, session $plantingSessionId" }

        val result = integrationService.triggerRecommendationAnalysis(
            userId,
            plantingSessionId,
            predictionResult
        )

        val emergencyResponse = EmergencyRecommendationResponse(
            plantingSessionId = plantingSessionId,
            criticalityLevel = when {
                result.criticalCount > 0 -> "CRITICAL"
                result.highPriorityCount > 0 -> "HIGH"
                else -> "MODERATE"
            },
            interventionRequired = result.interventionRequired,
            urgentActions = result.recommendations
                .filter { it.priority in listOf("CRITICAL", "HIGH") }
                .take(5), // Top 5 most urgent actions
            estimatedYieldImpact = calculateYieldImpact(predictionResult),
            recommendedNextSteps = generateNextSteps(result),
            analysisTimestamp = LocalDateTime.now()
        )

        logger.warn { "Emergency analysis completed: ${emergencyResponse.criticalityLevel} level, ${emergencyResponse.urgentActions.size} urgent actions" }
        return ResponseEntity.ok(emergencyResponse)
    }

    // ========================================
    // RECOMMENDATION MANAGEMENT ENDPOINTS
    // ========================================

    @PatchMapping("/{userId}/recommendation/{recommendationId}/mark-viewed")
    @Operation(
        summary = "Mark recommendation as viewed",
        description = "Marks a specific recommendation as viewed by the farmer. " +
                "This helps track which recommendations have been seen by the user."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Recommendation marked as viewed"),
            ApiResponse(responseCode = "404", description = "Recommendation not found"),
            ApiResponse(responseCode = "403", description = "Access denied to recommendation")
        ]
    )
    fun markRecommendationAsViewed(
        @Parameter(description = "User ID of the farm owner")
        @PathVariable userId: Long,
        @Parameter(description = "Recommendation ID to mark as viewed")
        @PathVariable recommendationId: Long
    ): ResponseEntity<RecommendationResponse> {
        logger.debug { "Marking recommendation $recommendationId as viewed for user $userId" }

        val recommendation = recommendationService.markRecommendationAsViewed(
            userId,
            recommendationId
        )

        return ResponseEntity.ok(recommendation)
    }

    @PatchMapping("/{userId}/recommendation/{recommendationId}/mark-implemented")
    @Operation(
        summary = "Mark recommendation as implemented",
        description = "Marks a specific recommendation as implemented by the farmer. " +
                "This helps track which recommendations have been acted upon."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Recommendation marked as implemented"),
            ApiResponse(responseCode = "404", description = "Recommendation not found"),
            ApiResponse(responseCode = "403", description = "Access denied to recommendation")
        ]
    )
    fun markRecommendationAsImplemented(
        @Parameter(description = "User ID of the farm owner")
        @PathVariable userId: Long,
        @Parameter(description = "Recommendation ID to mark as implemented")
        @PathVariable recommendationId: Long
    ): ResponseEntity<RecommendationResponse> {
        logger.info { "Marking recommendation $recommendationId as implemented for user $userId" }

        val recommendation = recommendationService.markRecommendationAsImplemented(
            userId,
            recommendationId
        )

        return ResponseEntity.ok(recommendation)
    }

    // ========================================
    // ANALYTICS AND SUMMARY ENDPOINTS
    // ========================================

    @GetMapping("/analysis-summary/{userId}/{plantingSessionId}")
    @Operation(
        summary = "Get recommendation analysis summary",
        description = "Retrieves a summary of the latest recommendation analysis for a planting session"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
            ApiResponse(responseCode = "404", description = "No analysis found for session")
        ]
    )
    fun getAnalysisSummary(
        @Parameter(description = "User ID of the farm owner")
        @PathVariable userId: Long,
        @Parameter(description = "Planting session ID")
        @PathVariable plantingSessionId: Long
    ): ResponseEntity<RecommendationSummary> {
        logger.debug { "Retrieving analysis summary for user $userId, session $plantingSessionId" }

        // Get current recommendations to build summary
        val recommendations = recommendationService.getRecommendationsByPlantingSession(userId, plantingSessionId)

        val summary = RecommendationSummary(
            plantingSessionId = plantingSessionId,
            lastAnalysisDate = LocalDateTime.now(),
            totalRecommendations = recommendations.size,
            criticalCount = recommendations.count { it.priority == "CRITICAL" },
            highPriorityCount = recommendations.count { it.priority == "HIGH" },
            mediumPriorityCount = recommendations.count { it.priority == "MEDIUM" },
            lowPriorityCount = recommendations.count { it.priority == "LOW" },
            interventionRequired = recommendations.any { it.priority in listOf("CRITICAL", "HIGH") },
            categories = recommendations.groupBy { it.category }.mapValues { it.value.size },
            viewedCount = recommendations.count { it.isViewed },
            implementedCount = recommendations.count { it.isImplemented }
        )

        return ResponseEntity.ok(summary)
    }

    // ========================================
    // CONFIGURATION ENDPOINTS
    // ========================================

    @GetMapping("/categories")
    @Operation(
        summary = "Get available recommendation categories",
        description = "Retrieves all available recommendation categories with their descriptions"
    )
    @ApiResponse(responseCode = "200", description = "Categories retrieved successfully")
    fun getRecommendationCategories(): ResponseEntity<Map<String, String>> {
        val categories = mapOf(
            "EMERGENCY" to "Critical situations requiring immediate action",
            "YIELD_OPTIMIZATION" to "Strategies to improve crop yield",
            "IRRIGATION" to "Water management and irrigation recommendations",
            "FERTILIZATION" to "Soil nutrient and fertilizer management",
            "PEST_CONTROL" to "Pest management and prevention",
            "DISEASE_MANAGEMENT" to "Disease prevention and treatment",
            "SOIL_MANAGEMENT" to "Soil health and management practices",
            "CROP_PROTECTION" to "General crop protection measures",
            "DRAINAGE" to "Field drainage and water management",
            "DATA_QUALITY" to "Data collection and quality improvements",
            "MONITORING" to "Regular monitoring and observation practices",
            "MAINTENANCE" to "General maintenance and upkeep"
        )
        return ResponseEntity.ok(categories)
    }

    @GetMapping("/priorities")
    @Operation(
        summary = "Get available priority levels",
        description = "Retrieves all available recommendation priority levels with their descriptions"
    )
    @ApiResponse(responseCode = "200", description = "Priority levels retrieved successfully")
    fun getRecommendationPriorities(): ResponseEntity<Map<String, String>> {
        val priorities = mapOf(
            "CRITICAL" to "Immediate action required within 24 hours",
            "HIGH" to "Action needed within 2-3 days",
            "MEDIUM" to "Action recommended within a week",
            "LOW" to "General maintenance, can be scheduled flexibly"
        )
        return ResponseEntity.ok(priorities)
    }

    @GetMapping("/risk-factors")
    @Operation(
        summary = "Get available risk factors",
        description = "Retrieves all risk factors that can trigger recommendations"
    )
    @ApiResponse(responseCode = "200", description = "Risk factors retrieved successfully")
    fun getRiskFactors(): ResponseEntity<Map<String, String>> {
        val riskFactors = mapOf(
            "DROUGHT_STRESS" to "Low rainfall or insufficient soil moisture",
            "HEAT_STRESS" to "High temperatures affecting crop growth",
            "NITROGEN_DEFICIENCY" to "Insufficient nitrogen in soil",
            "PHOSPHORUS_DEFICIENCY" to "Low phosphorus levels in soil",
            "PH_IMBALANCE" to "Soil pH outside optimal range",
            "EXCESSIVE_RAINFALL" to "Too much rainfall causing waterlogging",
            "SOIL_MOISTURE_LOW" to "Inadequate soil moisture levels",
            "LATE_SEASON_STRESS" to "Stress factors late in growing season"
        )
        return ResponseEntity.ok(riskFactors)
    }

    // ========================================
    // HEALTH CHECK ENDPOINT
    // ========================================

    @GetMapping("/health")
    @Operation(
        summary = "Recommendation service health check",
        description = "Checks the health and status of the recommendation service"
    )
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        val health = mapOf(
            "status" to "UP",
            "service" to "Recommendation Service",
            "timestamp" to LocalDateTime.now(),
            "version" to "1.0.0"
        )
        return ResponseEntity.ok(health)
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    private fun calculateYieldImpact(prediction: YieldPredictionResponse): Double {
        // Calculate percentage impact compared to optimal yield (6.0 tons/ha for Zimbabwe)
        val optimalYield = 6.0
        val impact = ((optimalYield - prediction.predictedYieldTonsPerHectare.toDouble()) / optimalYield) * 100
        return impact.coerceAtLeast(0.0) // Don't return negative impact
    }

    private fun generateNextSteps(result: RecommendationAnalysisResult): List<String> {
        val steps = mutableListOf<String>()

        if (result.criticalCount > 0) {
            steps.add("🚨 Implement critical recommendations immediately")
            steps.add("📅 Monitor crop daily for the next 7 days")
            steps.add("📞 Contact agricultural extension officer if needed")
        }

        if (result.highPriorityCount > 0) {
            steps.add("⏰ Schedule high-priority interventions within 48 hours")
            steps.add("🔍 Increase monitoring frequency")
        }

        if (result.interventionRequired) {
            steps.add("📋 Prepare emergency response plan")
            steps.add("💧 Check irrigation system functionality")
            steps.add("🌱 Assess crop health visually")
        }

        // Always include these general steps
        steps.add("📊 Update crop monitoring data regularly")
        steps.add("📈 Review recommendations weekly")
        steps.add("📝 Document any interventions implemented")

        return steps
    }
}

// ========================================
// DATA CLASSES FOR RESPONSES
// ========================================

/**
 * Summary of recommendation analysis for a planting session
 */
data class RecommendationSummary(
    val plantingSessionId: Long,
    val lastAnalysisDate: LocalDateTime,
    val totalRecommendations: Int,
    val criticalCount: Int,
    val highPriorityCount: Int,
    val mediumPriorityCount: Int,
    val lowPriorityCount: Int,
    val interventionRequired: Boolean,
    val categories: Map<String, Int>,
    val viewedCount: Int,
    val implementedCount: Int
)

/**
 * Emergency recommendation response with urgency indicators
 */
data class EmergencyRecommendationResponse(
    val plantingSessionId: Long,
    val criticalityLevel: String, // CRITICAL, HIGH, MODERATE
    val interventionRequired: Boolean,
    val urgentActions: List<RecommendationResponse>,
    val estimatedYieldImpact: Double, // Percentage impact
    val recommendedNextSteps: List<String>,
    val analysisTimestamp: LocalDateTime
)