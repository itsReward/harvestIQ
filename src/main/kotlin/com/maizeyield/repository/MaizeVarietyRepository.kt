package com.maizeyield.repository

import com.maizeyield.model.MaizeVariety
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface MaizeVarietyRepository : JpaRepository<MaizeVariety, Long> {
    fun findByName(name: String): Optional<MaizeVariety>
    fun findByDroughtResistance(droughtResistance: Boolean): List<MaizeVariety>
    fun findByMaturityDaysBetween(minDays: Int, maxDays: Int): List<MaizeVariety>
}