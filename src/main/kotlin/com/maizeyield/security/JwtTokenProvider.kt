package com.maizeyield.security

import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.security.Key
import java.util.*

private val logger = KotlinLogging.logger {}

@Component
class JwtTokenProvider {

    @Value("\${jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${jwt.expiration}")
    private var jwtExpirationInMs: Int = 86400000

    private val key: Key by lazy {
        Keys.hmacShaKeyFor(jwtSecret.toByteArray())
    }

    fun generateToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserPrincipal
        val expiryDate = Date(Date().time + jwtExpirationInMs)

        return Jwts.builder()
            .setSubject(userPrincipal.username) // Use username instead of ID
            .claim("userId", userPrincipal.id)   // Add user ID as custom claim
            .setIssuedAt(Date())
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact()
    }

    fun getUserIdFromToken(token: String): Long {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body

        return claims.subject.toLong()
    }

    fun validateToken(authToken: String): Boolean {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(authToken)
            return true
        } catch (ex: JwtException) {
            logger.error(ex) { "Invalid JWT token" }
        } catch (ex: IllegalArgumentException) {
            logger.error(ex) { "JWT claims string is empty" }
        }
        return false
    }

    /**
     * Extract username from JWT token
     * Note: Currently the token stores user ID in the subject, so we need to fetch the username from the user service
     * This method assumes you have a way to get username from user ID
     */
    fun getUsernameFromToken(token: String): String {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body

        // Since the subject contains the user ID, you might need to modify this
        // to store username directly in the token or fetch it from the database
        return claims.subject
    }

    /**
     * Get the expiration time in milliseconds
     */
    fun getExpirationInMs(): Int {
        return jwtExpirationInMs
    }
}