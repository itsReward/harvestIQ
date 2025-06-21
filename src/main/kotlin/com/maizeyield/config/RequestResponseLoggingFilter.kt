package com.maizeyield.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

@Component
class RequestResponseLoggingFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Skip logging for static resources and health checks
        if (shouldSkipLogging(request)) {
            filterChain.doFilter(request, response)
            return
        }

        val wrappedRequest = ContentCachingRequestWrapper(request)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } finally {
            logRequestAndResponse(wrappedRequest, wrappedResponse)
            wrappedResponse.copyBodyToResponse()
        }
    }

    private fun shouldSkipLogging(request: HttpServletRequest): Boolean {
        val uri = request.requestURI
        return uri.contains("/health") ||
                uri.contains("/actuator") ||
                uri.contains("/swagger") ||
                uri.contains("/api-docs") ||
                uri.endsWith(".css") ||
                uri.endsWith(".js") ||
                uri.endsWith(".png") ||
                uri.endsWith(".jpg") ||
                uri.endsWith(".ico")
    }

    private fun logRequestAndResponse(
        request: ContentCachingRequestWrapper,
        response: ContentCachingResponseWrapper
    ) {
        val requestBody = getRequestBody(request)
        val responseBody = getResponseBody(response)

        logger.info {
            buildString {
                appendLine("📋 === DETAILED REQUEST/RESPONSE LOG ===")
                appendLine("🔄 ${request.method} ${request.requestURI}")

                if (requestBody.isNotEmpty()) {
                    appendLine("📥 Request Body:")
                    appendLine(formatJson(requestBody))
                }

                appendLine("📤 Response Status: ${response.status}")

                if (responseBody.isNotEmpty() && response.status != 204) {
                    appendLine("📦 Response Body:")
                    appendLine(formatJson(responseBody))
                }

                appendLine("═".repeat(50))
            }
        }
    }

    private fun getRequestBody(request: ContentCachingRequestWrapper): String {
        val content = request.contentAsByteArray
        return if (content.isNotEmpty()) {
            String(content, StandardCharsets.UTF_8)
        } else ""
    }

    private fun getResponseBody(response: ContentCachingResponseWrapper): String {
        val content = response.contentAsByteArray
        return if (content.isNotEmpty()) {
            String(content, StandardCharsets.UTF_8)
        } else ""
    }

    private fun formatJson(content: String): String {
        return try {
            // Simple JSON formatting - add proper indentation
            if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
                content.take(500) + if (content.length > 500) "... (truncated)" else ""
            } else {
                content
            }
        } catch (e: Exception) {
            content
        }
    }
}
