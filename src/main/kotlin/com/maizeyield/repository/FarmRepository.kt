package com.maizeyield.repository

import com.maizeyield.model.Farm
import com.maizeyield.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.List

@Repository
interface FarmRepository : JpaRepository<Farm, Long> {
    fun findByUser(user: User): List<Farm>
    fun findByUserAndId(user: User, id: Long): Farm?
    fun countByUser(user: User): Long
}