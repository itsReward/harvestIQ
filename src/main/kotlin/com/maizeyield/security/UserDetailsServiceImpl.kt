package com.maizeyield.security

import com.maizeyield.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(private val userRepository: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user =  when (val response = userRepository.findByUsername(username)){
            null -> throw UsernameNotFoundException("User not found")
            else -> response
        }


        // For simplicity, we're using a single role "USER" for all users
        val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))

        return User(
            user.username,
            user.passwordHash,
            authorities
        )
    }
}