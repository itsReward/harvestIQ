package com.maizeyield.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(private val jwtTokenProvider: JwtTokenProvider) : OncePerRequestFilter() {

    private val logger = KotlinLogging.logger {}

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val jwt = resolveToken(request)
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt!!)) {
                val authentication = jwtTokenProvider.getAuthentication(jwt)
                SecurityContextHolder.getContext().authentication = authentication
                logger.debug { "Set Authentication to SecurityContext for user: ${authentication.name}" }
            }
        } catch (e: Exception) {
            logger.error { "Could not set user authentication in security context: ${e.message}" }
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}