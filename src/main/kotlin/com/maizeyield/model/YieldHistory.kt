package com.maizeyield.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "yield_history")
data class YieldHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planting_session_id", nullable = false)
    val plantingSession: PlantingSession,

    @Column(name = "harvest_date", nullable = false)
    val harvestDate: LocalDate,

    @Column(name = "yield_tons_per_hectare", nullable = false, precision = 6, scale = 2)
    val yieldTonsPerHectare: BigDecimal,

    @Column(name = "quality_rating", length = 20)
    val qualityRating: String? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

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

        other as YieldHistory

        if (id != other.id) return false
        if (plantingSession.id != other.plantingSession.id) return false
        if (harvestDate != other.harvestDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (plantingSession.id?.hashCode() ?: 0)
        result = 31 * result + harvestDate.hashCode()
        return result
    }

    override fun toString(): String {
        return "YieldHistory(id=$id, plantingSessionId=${plantingSession.id}, harvestDate=$harvestDate, yieldTonsPerHectare=$yieldTonsPerHectare)"
    }
}