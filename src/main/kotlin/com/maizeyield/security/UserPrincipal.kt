package com.maizeyield.security

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Custom UserPrincipal that implements Spring Security's UserDetails
 */
data class UserPrincipal(
    val id: Long,
    val name: String,
    private val username: String,
    val email: String,
    @JsonIgnore
    private val password: String,
    val roles: Collection<String> = emptyList()
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return roles.map { role ->
            SimpleGrantedAuthority("ROLE_$role")
        }
    }

    override fun getPassword(): String = password

    override fun getUsername(): String = username

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true

    companion object {
        fun create(user: com.maizeyield.model.User): UserPrincipal {
            return UserPrincipal(
                id = user.id!!,
                name = user.firstName!!,
                username = user.username,
                email = user.email,
                password = user.passwordHash,
                roles = listOf(user.role)
            )
        }
    }
}