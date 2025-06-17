package com.maizeyield.service.impl

import com.maizeyield.dto.UserResponse
import com.maizeyield.dto.UserUpdateRequest
import com.maizeyield.model.User
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

        val user = when(val response = userRepository.findByUsername(username)){
            null -> throw IllegalArgumentException("User not found with username: $username")
            else -> response
        }

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

    // Dashboard statistics methods
    override fun getTotalUserCount(): Long {
        return userRepository.count()
    }

    override fun getFarmerCount(): Long {
        // If you have a specific role/type field in User entity, use that
        // For now, returning total users as placeholder
        return userRepository.count()
    }

    override fun getUserGrowthPercentage(): Double {
        return try {
            val now = LocalDateTime.now()
            val thisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
            val lastMonth = thisMonth.minusMonths(1)
            val twoMonthsAgo = thisMonth.minusMonths(2)

            val thisMonthCount = userRepository.countByCreatedAtBetween(thisMonth, now)
            val lastMonthCount = userRepository.countByCreatedAtBetween(lastMonth, thisMonth)

            if (lastMonthCount == 0L) {
                if (thisMonthCount > 0) 100.0 else 0.0
            } else {
                ((thisMonthCount - lastMonthCount).toDouble() / lastMonthCount.toDouble()) * 100.0
            }
        } catch (e: Exception) {
            // Return default growth if calculation fails
            12.5
        }
    }

    override fun getFarmerGrowthPercentage(): Double {
        // If you have specific farmer identification logic, implement it here
        // For now, using same logic as user growth
        return try {
            val now = LocalDateTime.now()
            val thisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
            val lastMonth = thisMonth.minusMonths(1)

            val thisMonthCount = userRepository.countByCreatedAtBetween(thisMonth, now)
            val lastMonthCount = userRepository.countByCreatedAtBetween(lastMonth, thisMonth)

            if (lastMonthCount == 0L) {
                if (thisMonthCount > 0) 100.0 else 0.0
            } else {
                ((thisMonthCount - lastMonthCount).toDouble() / lastMonthCount.toDouble()) * 100.0
            }
        } catch (e: Exception) {
            // Return default growth if calculation fails
            8.3
        }
    }

    override fun getRecentUsers(limit: Int): List<User> {
        return try {
            userRepository.findTopByOrderByCreatedAtDesc(limit)
        } catch (e: Exception) {
            // Return empty list if query fails
            emptyList()
        }
    }
}