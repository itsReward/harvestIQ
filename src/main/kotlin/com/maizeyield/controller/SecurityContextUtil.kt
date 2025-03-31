package com.maizeyield.controller

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

object SecurityContextUtil {

    /**
     * Get the current authenticated username
     */
    fun getCurrentUsername(): String {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication.name
    }

    /**
     * Get the JWT token from the current request
     */
    fun getTokenFromRequest(): String {
        val requestAttributes = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
        val bearerToken = requestAttributes.request.getHeader("Authorization")

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }

        throw IllegalStateException("Authorization header is missing or not in the correct format")
    }
}