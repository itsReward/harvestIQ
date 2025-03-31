package com.maizeyield.controller

import com.maizeyield.dto.UserResponse
import com.maizeyield.dto.UserUpdateRequest
import com.maizeyield.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management operations")
class UserController(private val userService: UserService) {

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user profile")
    fun getCurrentUser(): ResponseEntity<UserResponse> {
        val user = userService.getCurrentUser()
        return ResponseEntity.ok(user)
    }

    @GetMapping("/{id}")
    @PreAuthorize("authentication.name == @userService.getUserById(#id).username")
    @Operation(summary = "Get user by ID")
    fun getUserById(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val user = userService.getUserById(id)
        return ResponseEntity.ok(user)
    }

    @PutMapping("/{id}")
    @PreAuthorize("authentication.name == @userService.getUserById(#id).username")
    @Operation(summary = "Update user profile")
    fun updateUser(
        @PathVariable id: Long,
        @Valid @RequestBody request: UserUpdateRequest
    ): ResponseEntity<UserResponse> {
        val updatedUser = userService.updateUser(id, request)
        return ResponseEntity.ok(updatedUser)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("authentication.name == @userService.getUserById(#id).username")
    @Operation(summary = "Delete user account")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        userService.deleteUser(id)
        return ResponseEntity.ok(mapOf("message" to "User deleted successfully"))
    }
}