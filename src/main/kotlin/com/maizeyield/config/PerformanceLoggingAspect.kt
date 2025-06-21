// src/main/kotlin/com/maizeyield/config/PerformanceLoggingAspect.kt
package com.maizeyield.config

import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

private val performanceLogger = KotlinLogging.logger("performance")

@Aspect
@Component
class PerformanceLoggingAspect {

    @Around("execution(* com.maizeyield.controller.*.*(..))")
    fun logControllerPerformance(joinPoint: ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()
        val className = joinPoint.target.javaClass.simpleName
        val methodName = joinPoint.signature.name

        return try {
            val result = joinPoint.proceed()
            val duration = System.currentTimeMillis() - startTime

            performanceLogger.info {
                "⚡ Controller Performance: $className.$methodName executed in ${duration}ms"
            }

            // Log slow controller methods
            if (duration > 1000) {
                performanceLogger.warn {
                    "🐌 SLOW CONTROLLER: $className.$methodName took ${duration}ms"
                }
            }

            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            performanceLogger.error {
                "❌ Controller Error: $className.$methodName failed after ${duration}ms - ${e.message}"
            }
            throw e
        }
    }

    @Around("execution(* com.maizeyield.service.*.*(..))")
    fun logServicePerformance(joinPoint: ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()
        val className = joinPoint.target.javaClass.simpleName
        val methodName = joinPoint.signature.name

        return try {
            val result = joinPoint.proceed()
            val duration = System.currentTimeMillis() - startTime

            if (duration > 500) { // Only log service calls that take more than 500ms
                performanceLogger.info {
                    "🔧 Service Performance: $className.$methodName executed in ${duration}ms"
                }
            }

            result
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            performanceLogger.error {
                "❌ Service Error: $className.$methodName failed after ${duration}ms - ${e.message}"
            }
            throw e
        }
    }
}