package com.maizeyield.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "yield_predictions")
data class YieldPrediction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planting_session_id", nullable = false)
    val plantingSession: PlantingSession,

    @Column(name = "prediction_date", nullable = false)
    val predictionDate: LocalDate,

    @Column(name = "predicted_yield_tons_per_hectare", nullable = false, precision = 6, scale = 2)
    val predictedYieldTonsPerHectare: BigDecimal,

    @Column(name = "confidence_percentage", nullable = false, precision = 5, scale = 2)
    val confidencePercentage: BigDecimal,

    @Column(name = "model_version", nullable = false, length = 50)
    val modelVersion: String,

    @Column(name = "features_used", nullable = false, columnDefinition = "TEXT")
    val featuresUsed: String,

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

        other as YieldPrediction

        if (id != other.id) return false
        if (plantingSession.id != other.plantingSession.id) return false
        if (predictionDate != other.predictionDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (plantingSession.id?.hashCode() ?: 0)
        result = 31 * result + predictionDate.hashCode()
        return result
    }

    override fun toString(): String {
        return "YieldPrediction(id=$id, plantingSessionId=${plantingSession.id}, predictionDate=$predictionDate, predictedYieldTonsPerHectare=$predictedYieldTonsPerHectare, confidencePercentage=$confidencePercentage)"
    }
}