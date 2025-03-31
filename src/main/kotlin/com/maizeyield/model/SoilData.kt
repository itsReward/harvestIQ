package com.maizeyield.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "soil_data")
data class SoilData(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    val farm: Farm,

    @Column(name = "soil_type", nullable = false, length = 50)
    var soilType: String,

    @Column(name = "ph_level", precision = 4, scale = 2)
    var phLevel: BigDecimal? = null,

    @Column(name = "organic_matter_percentage", precision = 5, scale = 2)
    var organicMatterPercentage: BigDecimal? = null,

    @Column(name = "nitrogen_content", precision = 5, scale = 2)
    var nitrogenContent: BigDecimal? = null,

    @Column(name = "phosphorus_content", precision = 5, scale = 2)
    var phosphorusContent: BigDecimal? = null,

    @Column(name = "potassium_content", precision = 5, scale = 2)
    var potassiumContent: BigDecimal? = null,

    @Column(name = "moisture_content", precision = 5, scale = 2)
    var moistureContent: BigDecimal? = null,

    @Column(name = "sample_date", nullable = false)
    var sampleDate: LocalDate,

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

        other as SoilData

        if (id != other.id) return false
        if (farm.id != other.farm.id) return false
        if (sampleDate != other.sampleDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (farm.id?.hashCode() ?: 0)
        result = 31 * result + sampleDate.hashCode()
        return result
    }

    override fun toString(): String {
        return "SoilData(id=$id, farmId=${farm.id}, soilType='$soilType', sampleDate=$sampleDate)"
    }
}