package com.maizeyield.repository

import com.maizeyield.model.PlantingSession
import com.maizeyield.model.YieldPrediction
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface YieldPredictionRepository : JpaRepository<YieldPrediction, Long> {
    fun findByPlantingSession(plantingSession: PlantingSession): List<YieldPrediction>

    fun findByPlantingSessionAndPredictionDate(plantingSession: PlantingSession, predictionDate: LocalDate): Optional<YieldPrediction>

    @Query("SELECT yp FROM YieldPrediction yp WHERE yp.plantingSession = :plantingSession ORDER BY yp.predictionDate DESC LIMIT 1")
    fun findLatestByPlantingSession(plantingSession: PlantingSession): Optional<YieldPrediction>

    @Query("SELECT AVG(yp.confidencePercentage) FROM YieldPrediction yp WHERE yp.plantingSession = :plantingSession AND yp.predictionDate BETWEEN :startDate AND :endDate")
    fun findAverageConfidenceByPlantingSessionAndDateRange(plantingSession: PlantingSession, startDate: LocalDate, endDate: LocalDate): Double?

    @Query("SELECT yp FROM YieldPrediction yp WHERE yp.plantingSession = :plantingSession AND yp.confidencePercentage >= :minConfidence ORDER BY yp.predictionDate DESC LIMIT 1")
    fun findLatestHighConfidencePrediction(plantingSession: PlantingSession, minConfidence: Double): Optional<YieldPrediction>

    // Dashboard query methods
    fun countByPredictionDateBetween(startDate: LocalDate, endDate: LocalDate): Long

    fun countByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long

    @Query("SELECT AVG(yp.predictedYieldTonsPerHectare) FROM YieldPrediction yp WHERE yp.predictionDate >= :sinceDate")
    fun findAverageYieldSince(@Param("sinceDate") sinceDate: LocalDate): Double?

    @Query("SELECT AVG(yp.predictedYieldTonsPerHectare) FROM YieldPrediction yp WHERE yp.predictionDate BETWEEN :startDate AND :endDate")
    fun findAverageYieldBetween(@Param("startDate") startDate: LocalDate, @Param("endDate") endDate: LocalDate): Double?

    @Query("SELECT yp FROM YieldPrediction yp ORDER BY yp.createdAt DESC LIMIT :limit")
    fun findTopByOrderByCreatedAtDesc(@Param("limit") limit: Int): List<YieldPrediction>


        // ADD THESE NEW METHODS:

        /**
         * Find yield predictions by planting session farm IDs with pagination
         * @param farmIds List of farm IDs to search for
         * @param pageable Pagination parameters
         * @return Page of yield predictions for the specified farms
         */
        @Query("SELECT yp FROM YieldPrediction yp WHERE yp.plantingSession.farm.id IN :farmIds")
        fun findByPlantingSessionFarmIdIn(farmIds: List<Long>, pageable: Pageable): Page<YieldPrediction>

        /**
         * Find yield predictions by planting session farm IDs without pagination
         * @param farmIds List of farm IDs to search for
         * @return List of yield predictions for the specified farms
         */
        @Query("SELECT yp FROM YieldPrediction yp WHERE yp.plantingSession.farm.id IN :farmIds ORDER BY yp.predictionDate DESC")
        fun findByPlantingSessionFarmIdIn(farmIds: List<Long>): List<YieldPrediction>

        /**
         * Find yield predictions by planting session with pagination
         * @param plantingSession The planting session
         * @param pageable Pagination parameters
         * @return Page of yield predictions for the planting session
         */
        fun findByPlantingSession(plantingSession: PlantingSession, pageable: Pageable): Page<YieldPrediction>

        /**
         * Find yield predictions by planting session and date range
         * @param plantingSession The planting session
         * @param startDate Start date for the range
         * @param endDate End date for the range
         * @return List of yield predictions within the date range
         */
        fun findByPlantingSessionAndPredictionDateBetween(
            plantingSession: PlantingSession,
            startDate: LocalDate,
            endDate: LocalDate
        ): List<YieldPrediction>

        /**
         * Find recent predictions by farm IDs with limit
         * @param farmIds List of farm IDs to search for
         * @param pageable Pagination parameters with sorting
         * @return List of recent yield predictions for the specified farms
         */
        @Query("SELECT yp FROM YieldPrediction yp WHERE yp.plantingSession.farm.id IN :farmIds ORDER BY yp.predictionDate DESC, yp.id DESC")
        fun findRecentPredictionsByFarmIds(farmIds: List<Long>, pageable: Pageable): List<YieldPrediction>

        /**
         * Find predictions with actual yield data for accuracy calculations
         * @return List of predictions paired with actual yield values
         */
        @Query("""
        SELECT yp, yh.yieldTonsPerHectare 
        FROM YieldPrediction yp 
        JOIN YieldHistory yh ON yp.plantingSession = yh.plantingSession 
        WHERE yh.yieldTonsPerHectare IS NOT NULL
    """)
        fun findPredictionsWithActualYield(): List<Array<YieldPrediction>>

        /**
         * Get confidence distribution for accuracy metrics
         * @return Map of confidence ranges and their counts
         */
        @Query("""
        SELECT 
            CASE 
                WHEN yp.confidencePercentage >= 90 THEN 'VERY_HIGH'
                WHEN yp.confidencePercentage >= 80 THEN 'HIGH'
                WHEN yp.confidencePercentage >= 70 THEN 'MEDIUM'
                WHEN yp.confidencePercentage >= 60 THEN 'LOW'
                ELSE 'VERY_LOW'
            END as confidenceRange,
            COUNT(yp) as count
        FROM YieldPrediction yp 
        GROUP BY 
            CASE 
                WHEN yp.confidencePercentage >= 90 THEN 'VERY_HIGH'
                WHEN yp.confidencePercentage >= 80 THEN 'HIGH'
                WHEN yp.confidencePercentage >= 70 THEN 'MEDIUM'
                WHEN yp.confidencePercentage >= 60 THEN 'LOW'
                ELSE 'VERY_LOW'
            END
    """)
        fun findConfidenceDistribution(): List<Array<Any>>

        /**
         * Find predictions by farm ID and date range
         * @param farmId The farm ID
         * @param startDate Start date for the range
         * @param endDate End date for the range
         * @return List of yield predictions within the date range for the farm
         */
        @Query("SELECT yp FROM YieldPrediction yp WHERE yp.plantingSession.farm.id = :farmId AND yp.predictionDate BETWEEN :startDate AND :endDate")
        fun findByFarmIdAndPredictionDateBetween(farmId: Long, startDate: LocalDate, endDate: LocalDate): List<YieldPrediction>

        /**
         * Count predictions by farm IDs
         * @param farmIds List of farm IDs
         * @return Total count of predictions for the specified farms
         */
        @Query("SELECT COUNT(yp) FROM YieldPrediction yp WHERE yp.plantingSession.farm.id IN :farmIds")
        fun countByPlantingSessionFarmIdIn(farmIds: List<Long>): Long

        /**
         * Find latest prediction for each farm
         * @param farmIds List of farm IDs
         * @return List of latest predictions for each farm
         */
        @Query("""
        SELECT yp FROM YieldPrediction yp 
        WHERE yp.plantingSession.farm.id IN :farmIds 
        AND yp.predictionDate = (
            SELECT MAX(yp2.predictionDate) 
            FROM YieldPrediction yp2 
            WHERE yp2.plantingSession.farm.id = yp.plantingSession.farm.id
        )
    """)
        fun findLatestPredictionsByFarmIds(farmIds: List<Long>): List<YieldPrediction>

        /**
         * Find predictions by model version
         * @param modelVersion The model version to filter by
         * @return List of predictions created with the specified model version
         */
        fun findByModelVersion(modelVersion: String): List<YieldPrediction>

        /**
         * Find predictions with confidence above threshold
         * @param farmIds List of farm IDs
         * @param confidenceThreshold Minimum confidence percentage
         * @return List of high-confidence predictions for the specified farms
         */
        @Query("SELECT yp FROM YieldPrediction yp WHERE yp.plantingSession.farm.id IN :farmIds AND yp.confidencePercentage >= :confidenceThreshold")
        fun findHighConfidencePredictionsByFarmIds(farmIds: List<Long>, confidenceThreshold: BigDecimal): List<YieldPrediction>

        /**
         * Get prediction statistics by farm ID
         * @param farmId The farm ID
         * @return Map containing various statistics for the farm's predictions
         */
        @Query("""
        SELECT 
            COUNT(yp) as totalPredictions,
            AVG(yp.predictedYieldTonsPerHectare) as averagePredictedYield,
            AVG(yp.confidencePercentage) as averageConfidence,
            MAX(yp.predictionDate) as latestPredictionDate,
            MIN(yp.predictionDate) as earliestPredictionDate
        FROM YieldPrediction yp 
        WHERE yp.plantingSession.farm.id = :farmId
    """)
        fun getPredictionStatisticsByFarmId(farmId: Long): List<Array<Any>>
}