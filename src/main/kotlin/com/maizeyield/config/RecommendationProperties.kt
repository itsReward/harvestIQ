package com.maizeyield.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "recommendation")
data class RecommendationProperties(
    val thresholds: Thresholds = Thresholds(),
    val categories: Map<String, CategoryConfig> = emptyMap(),
    val priorities: Map<String, PriorityConfig> = emptyMap(),
    val riskFactors: Map<String, RiskFactorConfig> = emptyMap(),
    val api: ApiConfig = ApiConfig(),
    val notifications: NotificationConfig = NotificationConfig(),
    val analytics: AnalyticsConfig = AnalyticsConfig(),
    val cache: CacheConfig = CacheConfig()
) {
    data class Thresholds(
        val yieldDeficitThresholdPercent: Double = 15.0,
        val lowConfidenceThreshold: Double = 65.0,
        val criticalYieldThreshold: Double = 2.0,
        val optimalYieldTarget: Double = 6.0,
        val weather: WeatherThresholds = WeatherThresholds(),
        val soil: SoilThresholds = SoilThresholds()
    )

    data class WeatherThresholds(
        val droughtThresholdDays: Int = 7,
        val heatStressTemperature: Double = 35.0,
        val excessiveRainfallThreshold: Double = 50.0,
        val lowRainfallThreshold: Double = 5.0
    )

    data class SoilThresholds(
        val phMin: Double = 5.5,
        val phMax: Double = 7.5,
        val nitrogenMin: Double = 20.0,
        val phosphorusMin: Double = 15.0,
        val moistureMin: Double = 30.0
    )

    data class CategoryConfig(
        val weight: Int = 5,
        val autoGenerate: Boolean = true
    )

    data class PriorityConfig(
        val notification: Boolean = false,
        val immediateAction: Boolean = false,
        val escalationHours: Int = 24
    )

    data class RiskFactorConfig(
        val severity: Int = 5,
        val category: String = "GENERAL",
        val priority: String = "MEDIUM",
        val autoRecommendations: List<String> = emptyList()
    )

    data class ApiConfig(
        val enabled: Boolean = false,
        val baseUrl: String = "",
        val apiKey: String = "",
        val timeoutSeconds: Int = 30,
        val retryAttempts: Int = 3,
        val fallbackToLocal: Boolean = true
    )

    data class NotificationConfig(
        val enabled: Boolean = true,
        val email: EmailConfig = EmailConfig(),
        val sms: SmsConfig = SmsConfig(),
        val push: PushConfig = PushConfig()
    ) {
        data class EmailConfig(
            val enabled: Boolean = true,
            val criticalOnly: Boolean = true
        )

        data class SmsConfig(
            val enabled: Boolean = false,
            val criticalOnly: Boolean = true
        )

        data class PushConfig(
            val enabled: Boolean = true,
            val allPriorities: Boolean = false
        )
    }

    data class AnalyticsConfig(
        val trackImplementation: Boolean = true,
        val trackEffectiveness: Boolean = true,
        val generateReports: Boolean = true,
        val retentionDays: Int = 365
    )

    data class CacheConfig(
        val enabled: Boolean = true,
        val ttlMinutes: Int = 60,
        val maxSize: Int = 1000
    )
}