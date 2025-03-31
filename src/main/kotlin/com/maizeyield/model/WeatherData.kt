package com.maizeyield.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "weather_data", uniqueConstraints = [UniqueConstraint(columnNames = ["farm_id", "date"])])
data class WeatherData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    val farm: Farm,

    @Column(nullable = false)
    val date: LocalDate,

    @Column(name = "min_temperature", precision = 4, scale = 1)
    var minTemperature: BigDecimal? = null,

    @Column(name = "max_temperature", precision = 4, scale = 1)
    var maxTemperature: BigDecimal? = null,

    @Column(name = "average_temperature", precision = 4, scale = 1)
    var averageTemperature: BigDecimal? = null,

    @Column(name = "rainfall_mm", precision = 6, scale = 2)
    var rainfallMm: BigDecimal? = null,

    @Column(name = "humidity_percentage", precision = 5, scale = 2)
    var humidityPercentage: BigDecimal? = null,

    @Column(name = "wind_speed_kmh", precision = 5, scale = 2)
    var windSpeedKmh: BigDecimal? = null,

    @Column(name = "solar_radiation", precision = 7, scale = 2)
    var solarRadiation: BigDecimal? = null,

    @Column(nullable = false, length = 50)
    val source: String,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WeatherData

        if (id != other.id) return false
        if (farm.id != other.farm.id) return false
        if (date != other.date) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (farm.id?.hashCode() ?: 0)
        result = 31 * result + date.hashCode()
        return result
    }

    override fun toString(): String {
        return "WeatherData(id=$id, farmId=${farm.id}, date=$date, source='$source')"
    }
}