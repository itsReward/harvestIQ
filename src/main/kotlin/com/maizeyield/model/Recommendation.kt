package com.maizeyield.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "recommendations")
data class Recommendation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "planting_session_id", nullable = false)
    val plantingSession: PlantingSession,

    @Column(name = "recommendation_date", nullable = false)
    val recommendationDate: LocalDate,

    @Column(nullable = false, length = 50)
    val category: String,

    @Column(nullable = false)
    val title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    val description: String,

    @Column(nullable = false, length = 20)
    val priority: String,

    @Column(name = "is_viewed", nullable = false)
    var isViewed: Boolean = false,

    @Column(name = "is_implemented", nullable = false)
    var isImplemented: Boolean = false,

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

        other as Recommendation

        if (id != other.id) return false
        if (plantingSession.id != other.plantingSession.id) return false
        if (recommendationDate != other.recommendationDate) return false
        if (title != other.title) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + (plantingSession.id?.hashCode() ?: 0)
        result = 31 * result + recommendationDate.hashCode()
        result = 31 * result + title.hashCode()
        return result
    }

    override fun toString(): String {
        return "Recommendation(id=$id, plantingSessionId=${plantingSession.id}, recommendationDate=$recommendationDate, category='$category', title='$title', priority='$priority')"
    }
}