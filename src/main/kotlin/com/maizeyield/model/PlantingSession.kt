package com.maizeyield.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "planting_sessions")
data class PlantingSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    val farm: Farm,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maize_variety_id", nullable = false)
    val maizeVariety: MaizeVariety,

    @Column(name = "planting_date", nullable = false)
    var plantingDate: LocalDate,

    @Column(name = "expected_harvest_date")
    var expectedHarvestDate: LocalDate? = null,

    @Column(name = "seed_rate_kg_per_hectare", precision = 6, scale = 2)
    var seedRateKgPerHectare: BigDecimal? = null,

    @Column(name = "row_spacing_cm")
    var rowSpacingCm: Int? = null,

    @Column(name = "fertilizer_type", length = 100)
    var fertilizerType: String? = null,

    @Column(name = "fertilizer_amount_kg_per_hectare", precision = 6, scale = 2)
    var fertilizerAmountKgPerHectare: BigDecimal? = null,

    @Column(name = "area_planted", precision = 6, scale = 2)
    var areaPlanted: BigDecimal? = null,

    @Column(name = "irrigation_method", length = 50)
    var irrigationMethod: String? = null,

    @Column(columnDefinition = "TEXT")
    var notes: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "plantingSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val yieldHistory: MutableList<YieldHistory> = mutableListOf(),

    @OneToMany(mappedBy = "plantingSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val yieldPredictions: MutableList<YieldPrediction> = mutableListOf(),

    @OneToMany(mappedBy = "plantingSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val recommendations: MutableList<Recommendation> = mutableListOf(),

    @OneToMany(mappedBy = "plantingSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val pestsDiseases: MutableList<PestsDiseaseMonitoring> = mutableListOf(),

    @OneToMany(mappedBy = "plantingSession", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val farmActivities: MutableList<FarmActivity> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlantingSession

        if (id != other.id) return false
        if (farm.id != other.farm.id) return false
        if (plantingDate != other.plantingDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (farm.id?.hashCode() ?: 0)
        result = 31 * result + plantingDate.hashCode()
        return result
    }

    override fun toString(): String {
        return "PlantingSession(id=$id, farmId=${farm.id}, maizeVarietyId=${maizeVariety.id}, plantingDate=$plantingDate)"
    }
}