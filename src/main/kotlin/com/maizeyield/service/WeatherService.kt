package com.maizeyield.service

import com.maizeyield.dto.WeatherDataCreateRequest
import com.maizeyield.dto.WeatherDataResponse
import com.maizeyield.dto.WeatherDataUpdateRequest
import com.maizeyield.dto.WeatherForecastResponse
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

interface WeatherService {
    /**
     * Get all weather data for a farm
     */
    fun getWeatherDataByFarmId(userId: Long, farmId: Long): List<WeatherDataResponse>
    
    /**
     * Get weather data for a specific date range
     */
    fun getWeatherDataByDateRange(userId: Long, farmId: Long, startDate: LocalDate, endDate: LocalDate): List<WeatherDataResponse>
    
    /**
     * Get weather data by ID
     */
    fun getWeatherDataById(userId: Long, weatherDataId: Long): WeatherDataResponse
    
    /**
     * Add weather data to a farm
     */
    fun addWeatherData(userId: Long, request: WeatherDataCreateRequest): WeatherDataResponse
    
    /**
     * Update weather data
     */
    fun updateWeatherData(userId: Long, weatherDataId: Long, request: WeatherDataUpdateRequest): WeatherDataResponse
    
    /**
     * Delete weather data
     */
    fun deleteWeatherData(userId: Long, weatherDataId: Long): Boolean
    
    /**
     * Get latest weather data for a farm
     */
    fun getLatestWeatherData(userId: Long, farmId: Long): WeatherDataResponse?
    
    /**
     * Fetch weather data from external API for a specific farm
     */
    fun fetchWeatherDataForFarm(userId: Long, farmId: Long): List<WeatherDataResponse>
    
    /**
     * Get weather forecast for a specific farm
     */
    fun getWeatherForecast(userId: Long, farmId: Long, days: Int): WeatherForecastResponse
    
    /**
     * Calculate average rainfall for a specific date range
     */
    fun calculateAverageRainfall(userId: Long, farmId: Long, startDate: LocalDate, endDate: LocalDate): BigDecimal?
    
    /**
     * Calculate average temperature for a specific date range
     */
    fun calculateAverageTemperature(userId: Long, farmId: Long, startDate: LocalDate, endDate: LocalDate): BigDecimal?

    /**
     * Get last update time
     */
    fun getLastUpdateTime(): LocalDateTime?
}
