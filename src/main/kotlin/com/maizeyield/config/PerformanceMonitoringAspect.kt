package com.maizeyield.config

import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Aspect
@Component
@ConditionalOnProperty(name = ["app.logging.performance.enabled"], havingValue = "true", matchIfMissing = true)
class PerformanceMonitoringAspect {

    private val logger = KotlinLogging.logger("performance")

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    fun logControllerExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()
        val methodName = "${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}"

        return try {
            val result = joinPoint.proceed()
            val executionTime = System.currentTimeMillis() - startTime

            logger.info {
                "🎯 Controller: $methodName executed in ${executionTime}ms ${getPerformanceEmoji(executionTime)}"
            }

            if (executionTime > 1000) {
                logger.warn {
                    "🐌 SLOW CONTROLLER: $methodName took ${executionTime}ms - Consider optimization"
                }
            }

            result
        } catch (exception: Throwable) {
            val executionTime = System.currentTimeMillis() - startTime
            logger.error(exception) {
                "💥 Controller: $methodName failed after ${executionTime}ms"
            }
            throw exception
        }
    }

    @Around("@within(org.springframework.stereotype.Service)")
    fun logServiceExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()
        val methodName = "${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}"

        return try {
            val result = joinPoint.proceed()
            val executionTime = System.currentTimeMillis() - startTime

            if (executionTime > 500) {
                logger.info {
                    "⚙️ Service: $methodName executed in ${executionTime}ms ${getPerformanceEmoji(executionTime)}"
                }
            }

            if (executionTime > 2000) {
                logger.warn {
                    "🐌 SLOW SERVICE: $methodName took ${executionTime}ms - Consider optimization"
                }
            }

            result
        } catch (exception: Throwable) {
            val executionTime = System.currentTimeMillis() - startTime
            logger.error(exception) {
                "💥 Service: $methodName failed after ${executionTime}ms"
            }
            throw exception
        }
    }

    @Around("@within(org.springframework.stereotype.Repository)")
    fun logRepositoryExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val startTime = System.currentTimeMillis()
        val methodName = "${joinPoint.signature.declaringTypeName}.${joinPoint.signature.name}"

        return try {
            val result = joinPoint.proceed()
            val executionTime = System.currentTimeMillis() - startTime

            if (executionTime > 100) {
                logger.info {
                    "🗄️ Repository: $methodName executed in ${executionTime}ms ${getPerformanceEmoji(executionTime)}"
                }
            }

            if (executionTime > 1000) {
                logger.warn {
                    "🐌 SLOW QUERY: $methodName took ${executionTime}ms - Consider database optimization"
                }
            }

            result
        } catch (exception: Throwable) {
            val executionTime = System.currentTimeMillis() - startTime
            logger.error(exception) {
                "💥 Repository: $methodName failed after ${executionTime}ms"
            }
            throw exception
        }
    }

    private fun getPerformanceEmoji(duration: Long): String {
        return when {
            duration < 50 -> "⚡"
            duration < 200 -> "✅"
            duration < 500 -> "⚠️"
            duration < 1000 -> "🐌"
            else -> "💀"
        }
    }
}