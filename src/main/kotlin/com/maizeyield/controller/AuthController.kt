package com.maizeyield.controller

import com.maizeyield.dto.LoginResponse
import com.maizeyield.dto.PasswordChangeRequest
import com.maizeyield.dto.UserLoginRequest
import com.maizeyield.dto.UserRegistrationRequest
import com.maizeyield.dto.UserResponse
import com.maizeyield.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication operations")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    fun registerUser(@Valid @RequestBody request: UserRegistrationRequest): ResponseEntity<UserResponse> {
        val user = authService.registerUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(user)
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and generate token")
    fun loginUser(@Valid @RequestBody request: UserLoginRequest): ResponseEntity<LoginResponse> {
        val loginResponse = authService.loginUser(request)
        return ResponseEntity.ok(loginResponse)
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change user password")
    fun changePassword(
        @Valid @RequestBody request: PasswordChangeRequest
    ): ResponseEntity<Map<String, String>> {
        val userId = authService.getUserIdFromToken(
            SecurityContextUtil.getTokenFromRequest()
        )

        authService.changePassword(userId, request)

        return ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
    }

    @GetMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Validate current authentication token")
    fun validateToken(): ResponseEntity<Map<String, Any>> {
        // If we reach here, the token is valid (Spring Security would have rejected invalid tokens)
        val userId = authService.getUserIdFromToken(
            SecurityContextUtil.getTokenFromRequest()
        )

        return ResponseEntity.ok(mapOf(
            "valid" to true,
            "userId" to userId,
            "timestamp" to System.currentTimeMillis()
        ))
    }

}