package com.example.commerce.point.repository

import com.example.commerce.point.entity.UserPoint
import org.springframework.data.jpa.repository.JpaRepository

interface UserPointRepository : JpaRepository<UserPoint, Long> {
    fun findByUserId(userId: Long): UserPoint?
}
