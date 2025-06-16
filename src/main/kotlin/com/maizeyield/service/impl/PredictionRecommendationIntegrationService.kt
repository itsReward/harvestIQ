package com.maizeyield.service.impl

import com.maizeyield.dto.RecommendationResponse
import com.maizeyield.dto.YieldPredictionResponse
import com.maizeyield.service.RecommendationService
import mu.KotlinLogging
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

/**
 * Service that automatically triggers recommendation generation when predictions are completed
 */
@Service
class PredictionRecommendationIntegrationService(
    private val recommendationService: RecommendationService
) {

    /**
     * Event listener that automatically triggers when a yield prediction is completed
     */
    @Async
    @EventListener
    @Transactional
    fun handlePredictionCompleted(event: YieldPredictionCompletedEvent) {
        logger.info { "Yield prediction completed for session ${event.plantingSessionId}, analyzing for recommendations..." }

        try {
            val recommendations = recommendationService.analyzeAndGenerateRecommendations(
                event.userId,
                event.plantingSessionId,
                event.predictionResult
            )

            logger.info { "Generated ${recommendations.size} recommendations for prediction analysis" }

            // Log critical recommendations for immediate attention
            val criticalRecommendations = recommendations.filter { it.priority == "CRITICAL" }
            if (criticalRecommendations.isNotEmpty()) {
                logger.warn { "CRITICAL recommendations generated: ${criticalRecommendations.size} items require immediate attention" }
                criticalRecommendations.forEach { recommendation ->
                    logger.warn { "CRITICAL: ${recommendation.title}" }
                }
            }

        } catch (e: Exception) {
            logger.error(e) { "Failed to generate recommendations for prediction analysis" }
        }
    }

    /**
     * Manual trigger for prediction analysis and recommendation generation
     */
    fun triggerRecommendationAnalysis(
        userId: Long,
        plantingSessionId: Long,
        predictionResult: YieldPredictionResponse
    ): RecommendationAnalysisResult {
        logger.info { "Manual trigger for recommendation analysis - Session: $plantingSessionId" }

        return try {
            val recommendations = recommendationService.analyzeAndGenerateRecommendations(
                userId,
                plantingSessionId,
                predictionResult
            )

            val analysisResult = categorizeRecommendations(recommendations)

            logger.info {
                "Recommendation analysis completed: ${analysisResult.totalRecommendations} total, " +
                        "${analysisResult.criticalCount} critical, ${analysisResult.highPriorityCount} high priority"
            }

            analysisResult

        } catch (e: Exception) {
            logger.error(e) { "Failed to analyze predictions and generate recommendations" }
            RecommendationAnalysisResult(
                success = false,
                errorMessage = e.message,
                recommendations = emptyList()
            )
        }
    }

    /**
     * Batch processing for multiple predictions
     */
    fun processBatchPredictionRecommendations(
        predictions: List<BatchPredictionItem>
    ): BatchRecommendationResult {
        logger.info { "Processing batch recommendations for ${predictions.size} predictions" }

        val results = mutableListOf<RecommendationAnalysisResult>()
        var successCount = 0
        var failureCount = 0

        predictions.forEach { item ->
            try {
                val result = triggerRecommendationAnalysis(
                    item.userId,
                    item.plantingSessionId,
                    item.predictionResult
                )
                results.add(result)
                if (result.success) successCount++ else failureCount++
            } catch (e: Exception) {
                logger.error(e) { "Failed to process recommendations for session ${item.plantingSessionId}" }
                failureCount++
                results.add(
                    RecommendationAnalysisResult(
                        success = false,
                        errorMessage = e.message,
                        recommendations = emptyList()
                    )
                )
            }
        }

        return BatchRecommendationResult(
            totalProcessed = predictions.size,
            successCount = successCount,
            failureCount = failureCount,
            results = results
        )
    }

    private fun categorizeRecommendations(recommendations: List<RecommendationResponse>): RecommendationAnalysisResult {
        val criticalCount = recommendations.count { it.priority == "CRITICAL" }
        val highPriorityCount = recommendations.count { it.priority == "HIGH" }
        val mediumPriorityCount = recommendations.count { it.priority == "MEDIUM" }
        val lowPriorityCount = recommendations.count { it.priority == "LOW" }

        val categorizedRecommendations = recommendations.groupBy { it.category }

        return RecommendationAnalysisResult(
            success = true,
            recommendations = recommendations,
            totalRecommendations = recommendations.size,
            criticalCount = criticalCount,
            highPriorityCount = highPriorityCount,
            mediumPriorityCount = mediumPriorityCount,
            lowPriorityCount = lowPriorityCount,
            categorizedRecommendations = categorizedRecommendations,
            interventionRequired = criticalCount > 0 || highPriorityCount > 0
        )
    }
}

/**
 * Event published when a yield prediction is completed
 */
data class YieldPredictionCompletedEvent(
    val userId: Long,
    val plantingSessionId: Long,
    val predictionResult: YieldPredictionResponse,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Result of recommendation analysis
 */
data class RecommendationAnalysisResult(
    val success: Boolean,
    val errorMessage: String? = null,
    val recommendations: List<RecommendationResponse> = emptyList(),
    val totalRecommendations: Int = 0,
    val criticalCount: Int = 0,
    val highPriorityCount: Int = 0,
    val mediumPriorityCount: Int = 0,
    val lowPriorityCount: Int = 0,
    val categorizedRecommendations: Map<String, List<RecommendationResponse>> = emptyMap(),
    val interventionRequired: Boolean = false
)

/**
 * Batch prediction item for processing
 */
data class BatchPredictionItem(
    val userId: Long,
    val plantingSessionId: Long,
    val predictionResult: YieldPredictionResponse
)

/**
 * Result of batch recommendation processing
 */
data class BatchRecommendationResult(
    val totalProcessed: Int,
    val successCount: Int,
    val failureCount: Int,
    val results: List<RecommendationAnalysisResult>
)