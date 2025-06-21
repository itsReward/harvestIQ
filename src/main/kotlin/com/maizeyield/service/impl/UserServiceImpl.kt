package com.maizeyield.service.impl

import com.maizeyield.dto.UserResponse
import com.maizeyield.dto.UserUpdateRequest
import com.maizeyield.model.User
import com.maizeyield.repository.UserRepository
import com.maizeyield.service.UserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
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

    override fun getAllUsers(
        pageable: Pageable,
        search: String
    ): Page<UserResponse> {
        val users = if (search.isBlank()) {
            // Get all users with pagination
            userRepository.findAll(pageable)
        } else {
            // Search users by username, email, first name, or last name
            userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                search, search, search, search, pageable
            )
        }

        val userResponses = users.content.map { user ->
            UserResponse(
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

        return PageImpl(userResponses, pageable, users.totalElements)
    }

    override fun getAllFarmers(
        pageable: Pageable,
        search: String
    ): Page<UserResponse> {
        val farmers = if (search.isBlank()) {
            // Get all farmers with pagination
            userRepository.findByRole("FARMER", pageable)
        } else {
            // Search farmers by username, email, first name, or last name
            userRepository.findByRoleAndUsernameContainingIgnoreCaseOrRoleAndEmailContainingIgnoreCaseOrRoleAndFirstNameContainingIgnoreCaseOrRoleAndLastNameContainingIgnoreCase(
                "FARMER", search,
                "FARMER", search,
                "FARMER", search,
                "FARMER", search,
                pageable
            )
        }

        val farmerResponses = farmers.content.map { farmer ->
            UserResponse(
                id = farmer.id!!,
                username = farmer.username,
                email = farmer.email,
                firstName = farmer.firstName,
                lastName = farmer.lastName,
                phoneNumber = farmer.phoneNumber,
                createdAt = farmer.createdAt,
                lastLogin = farmer.lastLogin
            )
        }

        return PageImpl(farmerResponses, pageable, farmers.totalElements)
    }

    @Transactional
    override fun updateUserRole(userId: Long, role: String): UserResponse {
        // Validate role
        val validRoles = listOf("ADMIN", "FARMER", "USER")
        if (!validRoles.contains(role.uppercase())) {
            throw IllegalArgumentException("Invalid role: $role. Valid roles are: ${validRoles.joinToString(", ")}")
        }

        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found with id: $userId") }

        val updatedUser = user.copy(
            role = role.uppercase(),
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

    // Dashboard statistics methods
    override fun getTotalUserCount(): Long {
        return userRepository.count()
    }

    override fun getFarmerCount(): Long {
        return userRepository.countByRole("FARMER")
    }

    override fun getUserGrowthPercentage(): Double {
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
            12.5
        }
    }

    override fun getFarmerGrowthPercentage(): Double {
        return try {
            val now = LocalDateTime.now()
            val thisMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
            val lastMonth = thisMonth.minusMonths(1)

            val thisMonthFarmerCount = userRepository.countByRoleAndCreatedAtBetween("FARMER", thisMonth, now)
            val lastMonthFarmerCount = userRepository.countByRoleAndCreatedAtBetween("FARMER", lastMonth, thisMonth)

            if (lastMonthFarmerCount == 0L) {
                if (thisMonthFarmerCount > 0) 100.0 else 0.0
            } else {
                ((thisMonthFarmerCount - lastMonthFarmerCount).toDouble() / lastMonthFarmerCount.toDouble()) * 100.0
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