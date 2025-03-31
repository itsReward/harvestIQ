package com.maizeyield.model

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "farms")
data class Farm(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 100)
    var location: String,

    @Column(name = "size_hectares", nullable = false, precision = 10, scale = 2)
    var sizeHectares: BigDecimal,

    @Column(precision = 10, scale = 6)
    var latitude: BigDecimal? = null,

    @Column(precision = 10, scale = 6)
    var longitude: BigDecimal? = null,

    @Column(precision = 10, scale = 2)
    var elevation: BigDecimal? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(mappedBy = "farm", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val soilData: MutableList<SoilData> = mutableListOf(),

    @OneToMany(mappedBy = "farm", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val plantingSessions: MutableList<PlantingSession> = mutableListOf(),

    @OneToMany(mappedBy = "farm", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val weatherData: MutableList<WeatherData> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Farm

        if (id != other.id) return false
        if (name != other.name) return false
        if (location != other.location) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        result = 31 * result + location.hashCode()
        return result
    }

    override fun toString(): String {
        return "Farm(id=$id, name='$name', location='$location', sizeHectares=$sizeHectares)"
    }
}