// src/main/kotlin/com/maizeyield/config/ApiLoggingInterceptor.kt
package com.maizeyield.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.nio.charset.StandardCharsets

@Component
class ApiLoggingInterceptor : HandlerInterceptor {
    private val logger = KotlinLogging.logger {}
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val startTime = System.currentTimeMillis()
        request.setAttribute("startTime", startTime)

        // Log incoming request
        logger.info {
            buildString {
                appendLine("🔄 === INCOMING API REQUEST ===")
                appendLine("📍 Method: ${request.method}")
                appendLine("🌐 URI: ${request.requestURI}")
                appendLine("❓ Query: ${request.queryString ?: "None"}")
                appendLine("🔑 Headers: ${getHeaders(request)}")
                appendLine("🏠 Remote Address: ${request.remoteAddr}")
                appendLine("👤 User Agent: ${request.getHeader("User-Agent") ?: "Unknown"}")
                appendLine("🕒 Timestamp: ${java.time.Instant.now()}")
            }
        }

        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        val startTime = request.getAttribute("startTime") as? Long ?: System.currentTimeMillis()
        val duration = System.currentTimeMillis() - startTime

        // Log response
        logger.info {
            buildString {
                appendLine("✅ === API RESPONSE ===")
                appendLine("📍 Method: ${request.method}")
                appendLine("🌐 URI: ${request.requestURI}")
                appendLine("📊 Status: ${response.status}")
                appendLine("⏱️ Duration: ${duration}ms")
                appendLine("📤 Response Headers: ${getResponseHeaders(response)}")

                if (ex != null) {
                    appendLine("❌ Exception: ${ex.message}")
                    appendLine("📚 Exception Type: ${ex.javaClass.simpleName}")
                }

                // Add performance warning for slow requests
                when {
                    duration > 5000 -> appendLine("🐌 SLOW REQUEST WARNING: ${duration}ms")
                    duration > 2000 -> appendLine("⚠️ PERFORMANCE WARNING: ${duration}ms")
                    duration < 100 -> appendLine("⚡ FAST REQUEST: ${duration}ms")
                }

                appendLine("🕒 Completed At: ${java.time.Instant.now()}")
            }
        }

        // Log error details if there was an exception
        if (ex != null) {
            logger.error(ex) {
                "💥 Exception during request processing: ${request.method} ${request.requestURI}"
            }
        }
    }

    private fun getHeaders(request: HttpServletRequest): String {
        return request.headerNames.asSequence()
            .filter { !it.equals("authorization", ignoreCase = true) } // Don't log auth tokens
            .joinToString(", ") { header ->
                "$header: ${request.getHeader(header)}"
            }
    }

    private fun getResponseHeaders(response: HttpServletResponse): String {
        return response.headerNames
            .filter { !it.equals("authorization", ignoreCase = true) }
            .joinToString(", ") { header ->
                "$header: ${response.getHeader(header)}"
            }
    }
}



