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

        // Wrap request to enable body reading
        val wrappedRequest = if (request !is ContentCachingRequestWrapper) {
            ContentCachingRequestWrapper(request)
        } else request

        // Log incoming request
        logger.info {
            buildString {
                appendLine("🔄 === INCOMING API REQUEST ===")
                appendLine("📍 Method: ${request.method}")
                appendLine("🌐 URI: ${request.requestURI}")
                appendLine("❓ Query: ${request.queryString ?: "None"}")
                appendLine("🔑 Headers: ${getHeaders(request)}")
                appendLine("📦 Request Body: ${getRequestBody(wrappedRequest)}")
                appendLine("🏠 Remote Address: ${request.remoteAddr}")
                appendLine("👤 User Agent: ${request.getHeader("User-Agent") ?: "Unknown"}")
                appendLine("🕒 Timestamp: ${java.time.Instant.now()}")
                appendLine("====================================================================================================================================================")
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

        // Wrap response to enable body reading
        val wrappedResponse = if (response !is ContentCachingResponseWrapper) {
            ContentCachingResponseWrapper(response)
        } else response

        // Log response
        logger.info {
            buildString {
                appendLine("✅ === API RESPONSE ===")
                appendLine("📍 Method: ${request.method}")
                appendLine("🌐 URI: ${request.requestURI}")
                appendLine("📊 Status: ${response.status} ${getStatusDescription(response.status)}")
                appendLine("⏱️ Duration: ${duration}ms ${getPerformanceEmoji(duration)}")
                appendLine("📤 Response Headers: ${getResponseHeaders(response)}")
                appendLine("📋 Response Body: ${getResponseBody(wrappedResponse)}")

                if (ex != null) {
                    appendLine("❌ Exception: ${ex.message}")
                    appendLine("❌ Exception Type: ${ex.javaClass.simpleName}")
                }

                appendLine("🕒 Completed At: ${java.time.Instant.now()}")
                appendLine("===================================================================================================================================================")
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
            .filter { !shouldHideHeader(it) }
            .joinToString("\n          ") { header ->
                "  $header: ${request.getHeader(header)}"
            }
    }

    private fun getResponseHeaders(response: HttpServletResponse): String {
        return response.headerNames
            .filter { !shouldHideHeader(it) }
            .joinToString("\n          ") { header ->
                "  $header: ${response.getHeader(header)}"
            }
    }

    private fun shouldHideHeader(headerName: String): Boolean {
        val lowerName = headerName.lowercase()
        return lowerName.contains("authorization") ||
                lowerName.contains("password") ||
                lowerName.contains("token") ||
                lowerName.contains("secret")
    }

    private fun getRequestBody(request: HttpServletRequest): String {
        return try {
            if (request is ContentCachingRequestWrapper) {
                val content = request.contentAsByteArray
                if (content.isNotEmpty()) {
                    val body = String(content, StandardCharsets.UTF_8)
                    if (body.length > 1000) {
                        "${body.take(1000)}... [truncated, full length: ${body.length}]"
                    } else {
                        body
                    }
                } else {
                    "[Empty]"
                }
            } else {
                "[Body not cached - add ContentCachingRequestWrapper]"
            }
        } catch (e: Exception) {
            "[Error reading body: ${e.message}]"
        }
    }

    private fun getResponseBody(response: HttpServletResponse): String {
        return try {
            if (response is ContentCachingResponseWrapper) {
                val content = response.contentAsByteArray
                if (content.isNotEmpty()) {
                    val body = String(content, StandardCharsets.UTF_8)
                    if (body.length > 1000) {
                        "${body.take(1000)}... [truncated, full length: ${body.length}]"
                    } else {
                        body
                    }
                } else {
                    "[Empty]"
                }
            } else {
                "[Body not cached - add ContentCachingResponseWrapper]"
            }
        } catch (e: Exception) {
            "[Error reading body: ${e.message}]"
        }
    }

    private fun getStatusDescription(status: Int): String {
        return when (status) {
            in 200..299 -> "SUCCESS"
            in 300..399 -> "REDIRECT"
            in 400..499 -> "CLIENT_ERROR"
            in 500..599 -> "SERVER_ERROR"
            else -> "UNKNOWN"
        }
    }

    private fun getPerformanceEmoji(duration: Long): String {
        return when {
            duration < 100 -> "⚡"
            duration < 500 -> "✅"
            duration < 1000 -> "⚠️"
            duration < 2000 -> "🐌"
            else -> "💀"
        }
    }
}