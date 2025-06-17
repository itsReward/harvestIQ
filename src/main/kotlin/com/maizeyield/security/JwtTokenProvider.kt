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
            .setSubject(userPrincipal.id.toString()) // Store user ID as subject
            .claim("username", userPrincipal.username) // Store username as custom claim
            .claim("email", userPrincipal.email) // Store email as custom claim
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

        // Now we can safely parse the subject as Long because it contains the user ID
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
     */
    fun getUsernameFromToken(token: String): String {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body

        // Get username from custom claim
        return claims.get("username", String::class.java)
    }

    /**
     * Extract email from JWT token
     */
    fun getEmailFromToken(token: String): String {
        val claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body

        // Get email from custom claim
        return claims.get("email", String::class.java)
    }

    /**
     * Get the expiration time in milliseconds
     */
    fun getExpirationInMs(): Int {
        return jwtExpirationInMs
    }
}