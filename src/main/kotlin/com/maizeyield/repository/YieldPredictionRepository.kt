package com.maizeyield.repository

import com.maizeyield.model.PlantingSession
import com.maizeyield.model.YieldPrediction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
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
}