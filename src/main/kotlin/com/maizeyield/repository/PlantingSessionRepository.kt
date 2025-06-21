package com.maizeyield.repository

import com.maizeyield.model.Farm
import com.maizeyield.model.MaizeVariety
import com.maizeyield.model.PlantingSession
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface PlantingSessionRepository : JpaRepository<PlantingSession, Long> {

    // Existing methods
    fun findByFarm(farm: Farm): List<PlantingSession>
    fun findByFarmAndPlantingDateBetween(farm: Farm, startDate: LocalDate, endDate: LocalDate): List<PlantingSession>
    fun existsByFarmAndPlantingDate(farm: Farm, plantingDate: LocalDate): Boolean
    fun findByFarmAndMaizeVariety(farm: Farm, maizeVariety: MaizeVariety): List<PlantingSession>

    @Query("SELECT ps FROM PlantingSession ps WHERE ps.farm = :farm AND ps.plantingDate <= :currentDate AND (ps.expectedHarvestDate IS NULL OR ps.expectedHarvestDate >= :currentDate)")
    fun findActiveSessionsByFarm(farm: Farm, currentDate: LocalDate): List<PlantingSession>

    @Query("SELECT ps FROM PlantingSession ps WHERE ps.farm = :farm ORDER BY ps.plantingDate DESC LIMIT 1")
    fun findLatestByFarm(farm: Farm): Optional<PlantingSession>

    // Dashboard query methods
    @Query("SELECT COUNT(ps) FROM PlantingSession ps WHERE ps.plantingDate >= :sinceDate")
    fun countActiveSessionsSince(@Param("sinceDate") sinceDate: LocalDate): Long

    fun countByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long

    @Query("SELECT ps FROM PlantingSession ps ORDER BY ps.createdAt DESC LIMIT :limit")
    fun findTopByOrderByCreatedAtDesc(@Param("limit") limit: Int): List<PlantingSession>

    // New methods for getAllPlantingSessions
    fun findByFarmIn(farms: List<Farm>): List<PlantingSession>

    fun findByFarmIn(farms: List<Farm>, pageable: Pageable): Page<PlantingSession>

    // Search methods for getAllPlantingSessions with search functionality
    @Query("SELECT ps FROM PlantingSession ps WHERE ps.farm IN :farms AND (LOWER(ps.farm.name) LIKE LOWER(CONCAT('%', :farmSearch, '%')) OR LOWER(ps.maizeVariety.name) LIKE LOWER(CONCAT('%', :varietySearch, '%')))")
    fun findByFarmInAndFarmNameContainingIgnoreCaseOrMaizeVarietyNameContainingIgnoreCase(
        @Param("farms") farms: List<Farm>,
        @Param("farmSearch") farmSearch: String,
        @Param("varietySearch") varietySearch: String,
        pageable: Pageable
    ): Page<PlantingSession>

    // Additional utility methods
    @Query("SELECT ps FROM PlantingSession ps WHERE ps.farm IN :farms AND ps.plantingDate BETWEEN :startDate AND :endDate")
    fun findByFarmInAndPlantingDateBetween(
        @Param("farms") farms: List<Farm>,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<PlantingSession>

    @Query("SELECT ps FROM PlantingSession ps WHERE ps.farm IN :farms AND ps.plantingDate <= :currentDate AND (ps.expectedHarvestDate IS NULL OR ps.expectedHarvestDate >= :currentDate)")
    fun findActiveSessionsByFarms(
        @Param("farms") farms: List<Farm>,
        @Param("currentDate") currentDate: LocalDate
    ): List<PlantingSession>

    // Status-related queries
    @Query("SELECT COUNT(ps) FROM PlantingSession ps WHERE ps.farm = :farm AND ps.plantingDate <= :currentDate AND (ps.expectedHarvestDate IS NULL OR ps.expectedHarvestDate >= :currentDate)")
    fun countActiveSessionsByFarm(@Param("farm") farm: Farm, @Param("currentDate") currentDate: LocalDate): Long

    @Query("SELECT COUNT(ps) FROM PlantingSession ps WHERE ps.farm IN :farms AND ps.plantingDate <= :currentDate AND (ps.expectedHarvestDate IS NULL OR ps.expectedHarvestDate >= :currentDate)")
    fun countActiveSessionsByFarms(@Param("farms") farms: List<Farm>, @Param("currentDate") currentDate: LocalDate): Long

    // Performance queries
    @Query("SELECT ps FROM PlantingSession ps WHERE ps.farm = :farm ORDER BY ps.createdAt DESC")
    fun findByFarmOrderByCreatedAtDesc(farm: Farm): List<PlantingSession>

    @Query("SELECT ps FROM PlantingSession ps WHERE ps.farm IN :farms ORDER BY ps.createdAt DESC")
    fun findByFarmInOrderByCreatedAtDesc(@Param("farms") farms: List<Farm>): List<PlantingSession>
}