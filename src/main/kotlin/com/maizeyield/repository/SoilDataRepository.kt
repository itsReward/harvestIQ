package com.maizeyield.repository

import com.maizeyield.model.Farm
import com.maizeyield.model.SoilData
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface SoilDataRepository : JpaRepository<SoilData, Long> {
    fun findByFarm(farm: Farm): List<SoilData>

    fun findByFarmAndSampleDate(farm: Farm, sampleDate: LocalDate): Optional<SoilData>

    @Query("SELECT s FROM SoilData s WHERE s.farm = :farm ORDER BY s.sampleDate DESC LIMIT 1")
    fun findLatestByFarm(farm: Farm): Optional<SoilData>
}