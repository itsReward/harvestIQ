package com.maizeyield.security

import com.maizeyield.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    @Transactional
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            ?: userRepository.findByEmail(username)
            ?: throw UsernameNotFoundException("User not found with username or email: $username")

        return UserPrincipal.create(user)
    }

    @Transactional
    fun loadUserById(id: Long): UserDetails {
        val user = userRepository.findById(id)
            .orElseThrow { UsernameNotFoundException("User not found with id: $id") }

        return UserPrincipal.create(user)
    }
}