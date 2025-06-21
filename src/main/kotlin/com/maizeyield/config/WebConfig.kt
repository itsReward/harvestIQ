// src/main/kotlin/com/maizeyield/config/WebConfig.kt
package com.maizeyield.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig @Autowired constructor(
    private val apiLoggingInterceptor: ApiLoggingInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(apiLoggingInterceptor)
            .addPathPatterns("/api/**") // Only log API endpoints
            .excludePathPatterns(
                "/api/health",           // Exclude health checks to reduce noise
                "/api/actuator/**",      // Exclude actuator endpoints
                "/api-docs/**",          // Exclude API documentation
                "/swagger-ui/**"         // Exclude Swagger UI
            )
    }
}