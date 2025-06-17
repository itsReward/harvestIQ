package com.maizeyield.controller

import com.maizeyield.dto.UserResponse
import com.maizeyield.dto.UserUpdateRequest
import com.maizeyield.service.AuthService
import com.maizeyield.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management operations")
class UserController(
    private val userService: UserService,
    private val authService: AuthService
) {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users (Admin only)")
    fun getAllUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "") search: String
    ): ResponseEntity<Page<UserResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val users = userService.getAllUsers(pageable, search)
        return ResponseEntity.ok(users)
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user profile")
    fun getCurrentUser(): ResponseEntity<UserResponse> {
        val user = userService.getCurrentUser()
        return ResponseEntity.ok(user)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == @userService.getUserById(#id).username")
    @Operation(summary = "Get user by ID")
    fun getUserById(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val user = userService.getUserById(id)
        return ResponseEntity.ok(user)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == @userService.getUserById(#id).username")
    @Operation(summary = "Update user profile")
    fun updateUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: UserUpdateRequest
    ): ResponseEntity<UserResponse> {
        val updatedUser = userService.updateUser(id, request)
        return ResponseEntity.ok(updatedUser)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == @userService.getUserById(#id).username")
    @Operation(summary = "Delete user account")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        userService.deleteUser(id)
        return ResponseEntity.ok(mapOf("message" to "User deleted successfully"))
    }

    @GetMapping("/farmers")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all farmers (Admin only)")
    fun getAllFarmers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "") search: String
    ): ResponseEntity<Page<UserResponse>> {
        val pageable: Pageable = PageRequest.of(page, size)
        val farmers = userService.getAllFarmers(pageable, search)
        return ResponseEntity.ok(farmers)
    }

    @PostMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user role (Admin only)")
    fun updateUserRole(
        @PathVariable userId: Long,
        @RequestParam role: String
    ): ResponseEntity<Map<String, String>> {
        userService.updateUserRole(userId, role)
        return ResponseEntity.ok(mapOf("message" to "User role updated successfully"))
    }
}