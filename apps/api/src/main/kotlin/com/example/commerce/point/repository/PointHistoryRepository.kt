package com.example.commerce.point.repository

import com.example.commerce.point.entity.PointHistory
import com.example.commerce.point.entity.PointHistoryType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface PointHistoryRepository : JpaRepository<PointHistory, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long, pageable: Pageable): Page<PointHistory>
    fun findByUserIdAndRelatedIdAndType(userId: Long, relatedId: Long, type: PointHistoryType): PointHistory?
}
