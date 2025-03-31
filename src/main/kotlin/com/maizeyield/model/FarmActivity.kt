package com.maizeyield.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "farm_activities")
data class FarmActivity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planting_session_id", nullable = false)
    val plantingSession: PlantingSession,

    @Column(name = "activity_date", nullable = false)
    val activityDate: LocalDate,

    @Column(name = "activity_type", nullable = false, length = 50)
    val activityType: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(name = "labor_hours", precision = 5, scale = 2)
    val laborHours: BigDecimal? = null,

    @Column(precision = 10, scale = 2)
    val cost: BigDecimal? = null,

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

        other as FarmActivity

        if (id != other.id) return false
        if (plantingSession.id != other.plantingSession.id) return false
        if (activityDate != other.activityDate) return false
        if (activityType != other.activityType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (plantingSession.id?.hashCode() ?: 0)
        result = 31 * result + activityDate.hashCode()
        result = 31 * result + activityType.hashCode()
        return result
    }

    override fun toString(): String {
        return "FarmActivity(id=$id, plantingSessionId=${plantingSession.id}, activityDate=$activityDate, activityType='$activityType')"
    }
}