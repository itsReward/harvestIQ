package com.maizeyield.repository

import com.maizeyield.model.PlantingSession
import com.maizeyield.model.Recommendation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface RecommendationRepository : JpaRepository<Recommendation, Long> {
    fun findByPlantingSession(plantingSession: PlantingSession): List<Recommendation>

    fun findByPlantingSessionAndRecommendationDate(plantingSession: PlantingSession, recommendationDate: LocalDate): List<Recommendation>

    fun findByPlantingSessionAndCategory(plantingSession: PlantingSession, category: String): List<Recommendation>

    fun findByPlantingSessionAndIsViewedFalse(plantingSession: PlantingSession): List<Recommendation>

    fun findByPlantingSessionAndIsImplementedFalse(plantingSession: PlantingSession): List<Recommendation>

    @Query("SELECT r FROM Recommendation r WHERE r.plantingSession = :plantingSession AND r.priority = :priority ORDER BY r.recommendationDate DESC")
    fun findByPlantingSessionAndPriorityOrderByRecommendationDateDesc(plantingSession: PlantingSession, priority: String): List<Recommendation>

    @Query("SELECT COUNT(r) FROM Recommendation r WHERE r.plantingSession = :plantingSession AND r.isImplemented = false AND r.priority IN ('HIGH', 'CRITICAL')")
    fun countUnimplementedHighPriorityRecommendations(plantingSession: PlantingSession): Long
}