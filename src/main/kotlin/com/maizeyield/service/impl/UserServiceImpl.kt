package com.maizeyield.service.impl

import com.maizeyield.dto.UserResponse
import com.maizeyield.dto.UserUpdateRequest
import com.maizeyield.repository.UserRepository
import com.maizeyield.service.UserService
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserServiceImpl(private val userRepository: UserRepository) : UserService {

    override fun getUserById(id: Long): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { IllegalArgumentException("User not found with id: $id") }

        return UserResponse(
            id = user.id!!,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            phoneNumber = user.phoneNumber,
            createdAt = user.createdAt,
            lastLogin = user.lastLogin
        )
    }

    override fun getCurrentUser(): UserResponse {
        val authentication = SecurityContextHolder.getContext().authentication
        val username = authentication.name

        val user = userRepository.findByUsername(username)
            .orElseThrow { IllegalArgumentException("User not found with username: $username") }

        return UserResponse(
            id = user.id!!,
            username = user.username,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            phoneNumber = user.phoneNumber,
            createdAt = user.createdAt,
            lastLogin = user.lastLogin
        )
    }

    @Transactional
    override fun updateUser(id: Long, request: UserUpdateRequest): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { IllegalArgumentException("User not found with id: $id") }

        // Only update fields that are not null in the request
        val updatedUser = user.copy(
            firstName = request.firstName ?: user.firstName,
            lastName = request.lastName ?: user.lastName,
            phoneNumber = request.phoneNumber ?: user.phoneNumber,
            email = request.email ?: user.email,
            updatedAt = LocalDateTime.now()
        )

        val savedUser = userRepository.save(updatedUser)

        return UserResponse(
            id = savedUser.id!!,
            username = savedUser.username,
            email = savedUser.email,
            firstName = savedUser.firstName,
            lastName = savedUser.lastName,
            phoneNumber = savedUser.phoneNumber,
            createdAt = savedUser.createdAt,
            lastLogin = savedUser.lastLogin
        )
    }

    @Transactional
    override fun deleteUser(id: Long): Boolean {
        if (!userRepository.existsById(id)) {
            throw IllegalArgumentException("User not found with id: $id")
        }

        userRepository.deleteById(id)
        return true
    }
}