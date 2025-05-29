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

    // New methods for external service integration
    fun findByFarmAndDateBefore(farm: Farm, date: LocalDate): List<WeatherData>

    fun findByFarmAndDateAfter(farm: Farm, date: LocalDate): List<WeatherData>

    @Query("SELECT w FROM WeatherData w WHERE w.farm = :farm AND w.source = :source ORDER BY w.date DESC")
    fun findByFarmAndSourceOrderByDateDesc(farm: Farm, source: String): List<WeatherData>

    @Query("SELECT DISTINCT w.source FROM WeatherData w WHERE w.farm = :farm")
    fun findDistinctSourcesByFarm(farm: Farm): List<String>

    @Query("SELECT w FROM WeatherData w WHERE w.farm = :farm AND w.date BETWEEN :startDate AND :endDate AND w.source = :source")
    fun findByFarmAndDateBetweenAndSource(farm: Farm, startDate: LocalDate, endDate: LocalDate, source: String): List<WeatherData>

    // Weather alerts and extreme conditions
    @Query("SELECT w FROM WeatherData w WHERE w.farm = :farm AND w.averageTemperature > :temperature")
    fun findByFarmAndTemperatureGreaterThan(farm: Farm, temperature: Double): List<WeatherData>

    @Query("SELECT w FROM WeatherData w WHERE w.farm = :farm AND w.rainfallMm > :rainfall")
    fun findByFarmAndRainfallGreaterThan(farm: Farm, rainfall: Double): List<WeatherData>

    @Query("SELECT w FROM WeatherData w WHERE w.farm = :farm AND w.windSpeedKmh > :windSpeed")
    fun findByFarmAndWindSpeedGreaterThan(farm: Farm, windSpeed: Double): List<WeatherData>

    // Statistics queries
    @Query("SELECT MAX(w.averageTemperature) FROM WeatherData w WHERE w.farm = :farm AND w.date BETWEEN :startDate AND :endDate")
    fun findMaxTemperatureByFarmAndDateRange(farm: Farm, startDate: LocalDate, endDate: LocalDate): Double?

    @Query("SELECT MIN(w.averageTemperature) FROM WeatherData w WHERE w.farm = :farm AND w.date BETWEEN :startDate AND :endDate")
    fun findMinTemperatureByFarmAndDateRange(farm: Farm, startDate: LocalDate, endDate: LocalDate): Double?

    @Query("SELECT SUM(w.rainfallMm) FROM WeatherData w WHERE w.farm = :farm AND w.date BETWEEN :startDate AND :endDate")
    fun findTotalRainfallByFarmAndDateRange(farm: Farm, startDate: LocalDate, endDate: LocalDate): Double?

    @Query("SELECT AVG(w.humidityPercentage) FROM WeatherData w WHERE w.farm = :farm AND w.date BETWEEN :startDate AND :endDate")
    fun findAverageHumidityByFarmAndDateRange(farm: Farm, startDate: LocalDate, endDate: LocalDate): Double?

    // Data quality and validation
    @Query("SELECT COUNT(w) FROM WeatherData w WHERE w.farm = :farm AND w.averageTemperature IS NULL")
    fun countMissingTemperatureRecords(farm: Farm): Long

    @Query("SELECT COUNT(w) FROM WeatherData w WHERE w.farm = :farm AND w.rainfallMm IS NULL")
    fun countMissingRainfallRecords(farm: Farm): Long

    // Recent weather patterns
    @Query("SELECT w FROM WeatherData w WHERE w.farm = :farm AND w.date >= :fromDate ORDER BY w.date DESC")
    fun findRecentWeatherData(farm: Farm, fromDate: LocalDate): List<WeatherData>

    // Weather streaks (consecutive dry/wet days)
    @Query("SELECT w FROM WeatherData w WHERE w.farm = :farm AND w.rainfallMm = 0 AND w.date BETWEEN :startDate AND :endDate ORDER BY w.date")
    fun findDryDaysInRange(farm: Farm, startDate: LocalDate, endDate: LocalDate): List<WeatherData>

    @Query("SELECT w FROM WeatherData w WHERE w.farm = :farm AND w.rainfallMm > 0 AND w.date BETWEEN :startDate AND :endDate ORDER BY w.date")
    fun findWetDaysInRange(farm: Farm, startDate: LocalDate, endDate: LocalDate): List<WeatherData>
}