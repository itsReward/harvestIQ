package com.maizeyield.service.impl

import com.maizeyield.dto.LoginResponse
import com.maizeyield.dto.UserLoginRequest
import com.maizeyield.dto.UserRegistrationRequest
import com.maizeyield.dto.UserResponse
import com.maizeyield.dto.PasswordChangeRequest
import com.maizeyield.model.User
import com.maizeyield.repository.UserRepository
import com.maizeyield.security.JwtTokenProvider
import com.maizeyield.service.AuthService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authenticationManager: AuthenticationManager
) : AuthService {

    @Transactional
    override fun registerUser(request: UserRegistrationRequest): UserResponse {
        // Check if username already exists
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username already exists")
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already exists")
        }

        // Create new user
        val user = User(
            username = request.username,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            firstName = request.firstName,
            lastName = request.lastName,
            phoneNumber = request.phoneNumber
        )

        val savedUser = userRepository.save(user)

        return UserResponse(
            id = savedUser.id!!,
            username = savedUser.username,
            email = savedUser.email,
            firstName = savedUser.firstName,
            lastName = savedUser.lastName,
            phoneNumber = savedUser.phoneNumber,
            createdAt = savedUser.createdAt
        )
    }

    override fun loginUser(request: UserLoginRequest): LoginResponse {
        val authentication: Authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password)
        )

        SecurityContextHolder.getContext().authentication = authentication

        val user = userRepository.findByUsername(request.username)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Update last login time
        user.lastLogin = LocalDateTime.now()
        userRepository.save(user)

        val jwt = jwtTokenProvider.generateToken(user.username)

        return LoginResponse(
            token = jwt,
            expiresIn = jwtTokenProvider.getExpirationInMs(),
            userId = user.id!!,
            username = user.username
        )
    }

    override fun validateToken(token: String): Boolean {
        return jwtTokenProvider.validateToken(token)
    }

    override fun getUserIdFromToken(token: String): Long {
        val username = jwtTokenProvider.getUsernameFromToken(token)
        val user = userRepository.findByUsername(username)
            .orElseThrow { IllegalArgumentException("User not found") }
        return user.id!!
    }

    @Transactional
    override fun changePassword(userId: Long, request: PasswordChangeRequest): Boolean {
        // Check if new password matches confirmation
        if (request.newPassword != request.confirmPassword) {
            throw IllegalArgumentException("New password and confirmation do not match")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Check if current password is correct
        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw IllegalArgumentException("Current password is incorrect")
        }

        // Update password
        val updatedUser = user.copy(
            passwordHash = passwordEncoder.encode(request.newPassword),
            updatedAt = LocalDateTime.now()
        )

        userRepository.save(updatedUser)
        return true
    }
}