package com.maizeyield.repository

import com.maizeyield.model.Farm
import com.maizeyield.model.MaizeVariety
import com.maizeyield.model.PlantingSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface PlantingSessionRepository : JpaRepository<PlantingSession, Long> {
    fun findByFarm(farm: Farm): List<PlantingSession>

    fun findByFarmAndPlantingDateBetween(farm: Farm, startDate: LocalDate, endDate: LocalDate): List<PlantingSession>

    @Query("SELECT ps FROM PlantingSession ps WHERE ps.farm = :farm AND ps.plantingDate <= :currentDate AND (ps.expectedHarvestDate IS NULL OR ps.expectedHarvestDate >= :currentDate)")
    fun findActiveSessionsByFarm(farm: Farm, currentDate: LocalDate): List<PlantingSession>

    @Query("SELECT ps FROM PlantingSession ps WHERE ps.farm = :farm ORDER BY ps.plantingDate DESC LIMIT 1")
    fun findLatestByFarm(farm: Farm): Optional<PlantingSession>

    fun existsByFarmAndPlantingDate(farm: Farm, plantingDate: LocalDate): Boolean

    fun findByFarmAndMaizeVariety(farm: Farm, maizeVariety: MaizeVariety): List<PlantingSession>
    // Dashboard query methods
    @Query("SELECT COUNT(ps) FROM PlantingSession ps WHERE ps.plantingDate >= :sinceDate")
    fun countActiveSessionsSince(@Param("sinceDate") sinceDate: LocalDate): Long

    fun countByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long

    @Query("SELECT ps FROM PlantingSession ps ORDER BY ps.createdAt DESC LIMIT :limit")
    fun findTopByOrderByCreatedAtDesc(@Param("limit") limit: Int): List<PlantingSession>
}