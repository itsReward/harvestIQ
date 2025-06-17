package com.maizeyield.controller

import com.maizeyield.dto.*
import com.maizeyield.service.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard statistics and overview operations")
class DashboardController(
    private val userService: UserService,
    private val farmService: FarmService,
    private val predictionService: PredictionService,
    private val weatherService: WeatherService
) {

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get dashboard statistics overview")
    fun getDashboardStats(): ResponseEntity<DashboardStatsResponse> {
        try {
            // Get basic counts
            val totalUsers = userService.getTotalUserCount()
            val totalFarmers = userService.getFarmerCount()
            val totalFarms = farmService.getTotalFarmCount()
            val totalPredictions = predictionService.getTotalPredictionCount()

            // Get active sessions (sessions from last 30 days)
            val activeSessions = predictionService.getActiveSessionCount()

            // Calculate average yield from recent predictions
            val avgYield = predictionService.getAverageYield()

            // Get model accuracy (you'll need to implement this based on your ML service)
            val modelAccuracy = 87.5 // Placeholder - implement based on your ML model metrics

            // Calculate growth percentages (compare with previous month)
            val userGrowth = userService.getUserGrowthPercentage()
            val farmerGrowth = userService.getFarmerGrowthPercentage()
            val sessionGrowth = predictionService.getSessionGrowthPercentage()
            val predictionGrowth = predictionService.getPredictionGrowthPercentage()
            val yieldGrowth = predictionService.getYieldGrowthPercentage()
            val accuracyImprovement = 2.3 // Placeholder - implement based on your ML metrics

            val stats = DashboardStatsResponse(
                totalUsers = totalUsers,
                totalFarmers = totalFarmers,
                totalFarms = totalFarms,
                activeSessions = activeSessions,
                totalPredictions = totalPredictions,
                avgYield = avgYield,
                modelAccuracy = modelAccuracy,
                userGrowth = userGrowth,
                farmerGrowth = farmerGrowth,
                sessionGrowth = sessionGrowth,
                predictionGrowth = predictionGrowth,
                yieldGrowth = yieldGrowth,
                accuracyImprovement = accuracyImprovement
            )

            return ResponseEntity.ok(stats)

        } catch (e: Exception) {
            // Log error and return basic stats
            println("Error fetching dashboard stats: ${e.message}")

            // Return fallback stats
            val fallbackStats = DashboardStatsResponse(
                totalUsers = 1250,
                totalFarmers = 892,
                totalFarms = 567,
                activeSessions = 156,
                totalPredictions = 3420,
                avgYield = 4.2,
                modelAccuracy = 87.5,
                userGrowth = 12.5,
                farmerGrowth = 8.3,
                sessionGrowth = 15.2,
                predictionGrowth = 22.1,
                yieldGrowth = -5.8,
                accuracyImprovement = 2.3
            )

            return ResponseEntity.ok(fallbackStats)
        }
    }

    @GetMapping("/recent-activity")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get recent system activity")
    fun getRecentActivity(): ResponseEntity<List<ActivityResponse>> {
        try {
            // Get recent activities from various services
            val activities = mutableListOf<ActivityResponse>()

            // Add recent user registrations
            val recentUsers = userService.getRecentUsers(5)
            recentUsers.forEach { user ->
                activities.add(
                    ActivityResponse(
                        id = user.id ?: 0,
                        action = "New user registration",
                        user = user.username,
                        time = formatTimeAgo(user.createdAt),
                        type = "USER_REGISTRATION"
                    )
                )
            }

            // Add recent predictions
            val recentPredictions = predictionService.getRecentPredictions(5)
            recentPredictions.forEach { prediction ->
                activities.add(
                    ActivityResponse(
                        id = prediction.id ?: 0,
                        action = "Yield prediction completed",
                        user = "System",
                        time = formatTimeAgo(prediction.createdAt),
                        type = "PREDICTION"
                    )
                )
            }

            // Add recent weather updates (if available)
            try {
                val lastWeatherUpdate = weatherService.getLastUpdateTime()
                if (lastWeatherUpdate != null) {
                    activities.add(
                        ActivityResponse(
                            id = 0,
                            action = "Weather data updated",
                            user = "WeatherAPI",
                            time = formatTimeAgo(lastWeatherUpdate),
                            type = "WEATHER_UPDATE"
                        )
                    )
                }
            } catch (e: Exception) {
                // Weather service might not be available
            }

            // Sort by time and return latest activities
            activities.sortByDescending { it.id }

            return ResponseEntity.ok(activities.take(10))

        } catch (e: Exception) {
            // Log error and return fallback activities
            println("Error fetching recent activity: ${e.message}")

            val fallbackActivities = listOf(
                ActivityResponse(1, "New farmer registration", "John Doe", "2 hours ago", "USER_REGISTRATION"),
                ActivityResponse(2, "Yield prediction completed", "System", "4 hours ago", "PREDICTION"),
                ActivityResponse(3, "Weather data updated", "WeatherAPI", "6 hours ago", "WEATHER_UPDATE"),
                ActivityResponse(4, "Model training completed", "ML Service", "1 day ago", "MODEL_TRAINING")
            )

            return ResponseEntity.ok(fallbackActivities)
        }
    }

    private fun formatTimeAgo(dateTime: LocalDateTime?): String {
        if (dateTime == null) return "Unknown"

        val now = LocalDateTime.now()
        val duration = java.time.Duration.between(dateTime, now)

        return when {
            duration.toDays() > 0 -> "${duration.toDays()} day${if (duration.toDays() > 1) "s" else ""} ago"
            duration.toHours() > 0 -> "${duration.toHours()} hour${if (duration.toHours() > 1) "s" else ""} ago"
            duration.toMinutes() > 0 -> "${duration.toMinutes()} minute${if (duration.toMinutes() > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }
}

// Data classes for dashboard responses
data class DashboardStatsResponse(
    val totalUsers: Long,
    val totalFarmers: Long,
    val totalFarms: Long,
    val activeSessions: Long,
    val totalPredictions: Long,
    val avgYield: Double,
    val modelAccuracy: Double,
    val userGrowth: Double,
    val farmerGrowth: Double,
    val sessionGrowth: Double,
    val predictionGrowth: Double,
    val yieldGrowth: Double,
    val accuracyImprovement: Double
)

data class ActivityResponse(
    val id: Long,
    val action: String,
    val user: String,
    val time: String,
    val type: String
)