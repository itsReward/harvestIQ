package com.maizeyield.repository

import com.maizeyield.model.Farm
import com.maizeyield.model.WeatherData
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface WeatherDataRepository : JpaRepository<WeatherData, Long> {
    fun findByFarmAndDate(farm: Farm, date: LocalDate): Optional<WeatherData>

    fun findByFarm(farm: Farm): List<WeatherData>

    fun findByFarmAndDateBetween(farm: Farm, startDate: LocalDate, endDate: LocalDate): List<WeatherData>

    @Query("SELECT AVG(w.rainfallMm) FROM WeatherData w WHERE w.farm = :farm AND w.date BETWEEN :startDate AND :endDate")
    fun findAverageRainfallByFarmAndDateRange(farm: Farm, startDate: LocalDate, endDate: LocalDate): Double?

    @Query("SELECT AVG(w.averageTemperature) FROM WeatherData w WHERE w.farm = :farm AND w.date BETWEEN :startDate AND :endDate")
    fun findAverageTemperatureByFarmAndDateRange(farm: Farm, startDate: LocalDate, endDate: LocalDate): Double?

    @Query("SELECT w FROM WeatherData w WHERE w.farm = :farm ORDER BY w.date DESC LIMIT 1")
    fun findLatestByFarm(farm: Farm): Optional<WeatherData>

    @Query("SELECT COUNT(w) FROM WeatherData w WHERE w.farm = :farm AND w.date BETWEEN :startDate AND :endDate")
    fun countRecordsInDateRange(farm: Farm, startDate: LocalDate, endDate: LocalDate): Long
}