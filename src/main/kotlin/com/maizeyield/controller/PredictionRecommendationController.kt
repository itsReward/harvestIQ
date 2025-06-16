package com.maizeyield.controller

import com.maizeyield.dto.YieldPredictionResponse
import com.maizeyield.security.CurrentUser
import com.maizeyield.security.UserPrincipal
import com.maizeyield.service.impl.PredictionRecommendationIntegrationService
import com.maizeyield.service.impl.BatchPredictionItem
import com.maizeyield.service.impl.BatchRecommendationResult
import com.maizeyield.service.impl.RecommendationAnalysisResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/v1/prediction-recommendations")
@Tag(name = "Prediction-Recommendation Integration", description = "APIs for automated recommendation generation based on yield predictions")
class PredictionRecommendationController(
    private val integrationService: PredictionRecommendationIntegrationService
) {

    @PostMapping("/analyze/{plantingSessionId}")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    @Operation(
        summary = "Trigger recommendation analysis for prediction",
        description = "Manually triggers recommendation analysis and generation based on a yield prediction result"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Analysis completed successfully"),
            ApiResponse(responseCode = "400", description = "Invalid prediction data"),
            ApiResponse(responseCode = "404", description = "Planting session not found"),
            ApiResponse(responseCode = "403", description = "Access denied")
        ]
    )
    fun triggerRecommendationAnalysis(
        @Parameter(description = "Planting session ID")
        @PathVariable plantingSessionId: Long,
        @Parameter(description = "Yield prediction result to analyze")
        @Valid @RequestBody predictionResult: YieldPredictionResponse,
        @CurrentUser userPrincipal: UserPrincipal
    ): ResponseEntity<RecommendationAnalysisResult> {
        val result = integrationService.triggerRecommendationAnalysis(
            userPrincipal.id,
            plantingSessionId,
            predictionResult
        )
        return ResponseEntity.ok(result)
    }

    @PostMapping("/batch-analyze")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    @Operation(
        summary = "Batch process prediction recommendations",
        description = "Processes multiple predictions and generates recommendations in batch"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Batch processing completed"),
            ApiResponse(responseCode = "400", description = "Invalid batch data"),
            ApiResponse(responseCode = "403", description = "Access denied")
        ]
    )
    fun processBatchRecommendations(
        @Parameter(description = "List of predictions to process")
        @Valid @RequestBody predictions: List<BatchPredictionItem>,
        @CurrentUser userPrincipal: UserPrincipal
    ): ResponseEntity<BatchRecommendationResult> {
        // Validate that all predictions belong to the user
        val unauthorizedItems = predictions.filter { it.userId != userPrincipal.id }
        if (unauthorizedItems.isNotEmpty()) {
            return ResponseEntity.badRequest().build()
        }

        val result = integrationService.processBatchPredictionRecommendations(predictions)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/analysis-summary/{plantingSessionId}")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get recommendation analysis summary",
        description = "Retrieves a summary of the latest recommendation analysis for a planting session"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Summary retrieved successfully"),
            ApiResponse(responseCode = "404", description = "No analysis found"),
            ApiResponse(responseCode = "403", description = "Access denied")
        ]
    )
    fun getAnalysisSummary(
        @Parameter(description = "Planting session ID")
        @PathVariable plantingSessionId: Long,
        @CurrentUser userPrincipal: UserPrincipal
    ): ResponseEntity<PredictionRecommendationSummary> {
        // This would typically fetch from a cache or database
        // For now, return a placeholder implementation
        val summary = PredictionRecommendationSummary(
            plantingSessionId = plantingSessionId,
            lastAnalysisDate = java.time.LocalDateTime.now(),
            totalRecommendations = 0,
            criticalCount = 0,
            interventionRequired = false,
            categories = emptyMap()
        )
        return ResponseEntity.ok(summary)
    }

    @PostMapping("/emergency-analysis/{plantingSessionId}")
    @PreAuthorize("hasRole('FARMER') or hasRole('ADMIN')")
    @Operation(
        summary = "Emergency recommendation analysis",
        description = "Triggers emergency analysis when critical yield thresholds are detected"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Emergency analysis completed"),
            ApiResponse(responseCode = "404", description = "Planting session not found"),
            ApiResponse(responseCode = "403", description = "Access denied")
        ]
    )
    fun emergencyAnalysis(
        @Parameter(description = "Planting session ID")
        @PathVariable plantingSessionId: Long,
        @Parameter(description = "Critical prediction result")
        @Valid @RequestBody predictionResult: YieldPredictionResponse,
        @CurrentUser userPrincipal: UserPrincipal
    ): ResponseEntity<PredictionEmergencyRecommendationResponse> {
        val result = integrationService.triggerRecommendationAnalysis(
            userPrincipal.id,
            plantingSessionId,
            predictionResult
        )

        val emergencyResponse = PredictionEmergencyRecommendationResponse(
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
            recommendedNextSteps = generateNextSteps(result)
        )

        return ResponseEntity.ok(emergencyResponse)
    }

    private fun calculateYieldImpact(prediction: YieldPredictionResponse): Double {
        // Simplified calculation - could be more sophisticated
        return when {
            prediction.predictedYieldTonsPerHectare < 2.0.toBigDecimal() -> -70.0 // 70% below optimal
            prediction.predictedYieldTonsPerHectare < 4.0.toBigDecimal() -> -40.0 // 40% below optimal
            prediction.predictedYieldTonsPerHectare < 5.0.toBigDecimal() -> -20.0 // 20% below optimal
            else -> 0.0
        }
    }

    private fun generateNextSteps(result: RecommendationAnalysisResult): List<String> {
        val steps = mutableListOf<String>()

        if (result.criticalCount > 0) {
            steps.add("Implement critical recommendations immediately")
            steps.add("Monitor crop daily for the next 7 days")
        }

        if (result.highPriorityCount > 0) {
            steps.add("Schedule high-priority interventions within 48 hours")
        }

        if (result.interventionRequired) {
            steps.add("Prepare emergency response plan")
            steps.add("Contact agricultural extension officer if needed")
        }

        steps.add("Update crop monitoring data regularly")
        steps.add("Review recommendations weekly")

        return steps
    }
}

/**
 * Summary of recommendation analysis
 */
data class PredictionRecommendationSummary(
    val plantingSessionId: Long,
    val lastAnalysisDate: java.time.LocalDateTime,
    val totalRecommendations: Int,
    val criticalCount: Int,
    val interventionRequired: Boolean,
    val categories: Map<String, Int>
)

/**
 * Emergency recommendation response
 */
data class PredictionEmergencyRecommendationResponse(
    val plantingSessionId: Long,
    val criticalityLevel: String,
    val interventionRequired: Boolean,
    val urgentActions: List<com.maizeyield.dto.RecommendationResponse>,
    val estimatedYieldImpact: Double,
    val recommendedNextSteps: List<String>
)