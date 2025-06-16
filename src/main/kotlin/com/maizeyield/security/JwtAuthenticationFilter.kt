package com.maizeyield.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.springframework.web.filter.OncePerRequestFilter

private val logger = KotlinLogging.logger {}

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val customUserDetailsService: CustomUserDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // Skip JWT processing for auth endpoints
            if (request.requestURI.startsWith("/api/auth/")) {
                filterChain.doFilter(request, response)
                return
            }

            // Check if authentication is already set to prevent recursion
            if (SecurityContextHolder.getContext().authentication != null) {
                filterChain.doFilter(request, response)
                return
            }

            val jwt = getJwtFromRequest(request)

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt!!)) {
                val userId = jwtTokenProvider.getUserIdFromToken(jwt)

                // Use a simple try-catch to prevent recursion
                try {
                    val userDetails = customUserDetailsService.loadUserById(userId)
                    val authentication = UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.authorities
                    )
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)

                    SecurityContextHolder.getContext().authentication = authentication
                } catch (userEx: Exception) {
                    logger.error("Failed to load user for authentication", userEx)
                    // Clear any partial authentication
                    SecurityContextHolder.clearContext()
                }
            }
        } catch (ex: Exception) {
            logger.error("Could not set user authentication in security context", ex)
            // Clear security context on any error
            SecurityContextHolder.clearContext()
        }

        filterChain.doFilter(request, response)
    }

    private fun getJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }
}