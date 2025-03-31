package com.maizeyield.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "maize_varieties")
data class MaizeVariety(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "maturity_days", nullable = false)
    var maturityDays: Int,

    @Column(name = "optimal_temperature_min", precision = 4, scale = 1)
    var optimalTemperatureMin: BigDecimal? = null,

    @Column(name = "optimal_temperature_max", precision = 4, scale = 1)
    var optimalTemperatureMax: BigDecimal? = null,

    @Column(name = "drought_resistance", nullable = false)
    var droughtResistance: Boolean = false,

    @Column(name = "disease_resistance", length = 100)
    var diseaseResistance: String? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "maizeVariety", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val plantingSessions: MutableList<PlantingSession> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MaizeVariety

        if (id != other.id) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "MaizeVariety(id=$id, name='$name', maturityDays=$maturityDays)"
    }
}