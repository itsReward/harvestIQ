package com.maizeyield.repository

import com.maizeyield.model.PlantingSession
import com.maizeyield.model.YieldPrediction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
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
}