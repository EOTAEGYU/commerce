package com.example.commerce.point.entity

import com.example.commerce.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "user_points")
class UserPoint(
    @Column(nullable = false, unique = true)
    val userId: Long,

    @Column(nullable = false)
    var balance: Long = 0,

    @Version
    @Column(nullable = false)
    val version: Long = 0,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity() {
    companion object {
        fun create(userId: Long): UserPoint = UserPoint(userId = userId)
    }
}
