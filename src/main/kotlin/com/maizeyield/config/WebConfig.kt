package com.maizeyield.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
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

    // ✅ Additional CORS configuration at MVC level for backup
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:3001"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600)
    }
}