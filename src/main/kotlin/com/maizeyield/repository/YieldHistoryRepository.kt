package com.maizeyield.repository

import com.maizeyield.model.Farm
import com.maizeyield.model.PlantingSession
import com.maizeyield.model.YieldHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@Repository
interface YieldHistoryRepository : JpaRepository<YieldHistory, Long> {
    fun findByPlantingSession(plantingSession: PlantingSession): List<YieldHistory>

    @Query("SELECT yh FROM YieldHistory yh WHERE yh.plantingSession.farm = :farm")
    fun findByFarm(farm: Farm): List<YieldHistory>

    @Query("SELECT yh FROM YieldHistory yh WHERE yh.plantingSession.farm = :farm AND yh.harvestDate BETWEEN :startDate AND :endDate")
    fun findByFarmAndHarvestDateBetween(farm: Farm, startDate: LocalDate, endDate: LocalDate): List<YieldHistory>

    @Query("SELECT AVG(yh.yieldTonsPerHectare) FROM YieldHistory yh WHERE yh.plantingSession.farm = :farm")
    fun findAverageYieldByFarm(farm: Farm): BigDecimal?

    @Query("SELECT yh FROM YieldHistory yh WHERE yh.plantingSession.farm = :farm ORDER BY yh.yieldTonsPerHectare DESC LIMIT 1")
    fun findHighestYieldByFarm(farm: Farm): Optional<YieldHistory>

    @Query("SELECT yh FROM YieldHistory yh WHERE yh.plantingSession = :plantingSession ORDER BY yh.harvestDate DESC LIMIT 1")
    fun findLatestByPlantingSession(plantingSession: PlantingSession): Optional<YieldHistory>

    @Query("SELECT COUNT(yh) FROM YieldHistory yh WHERE yh.plantingSession.farm = :farm")
    fun countByFarm(farm: Farm): Long

    @Query("SELECT yh FROM YieldHistory yh WHERE yh.plantingSession.farm = :farm ORDER BY yh.harvestDate DESC")
    fun findByFarmOrderByHarvestDateDesc(farm: Farm): List<YieldHistory>
}