package com.example.commerce.user.entity

import com.example.commerce.common.BaseEntity
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(name = "users")
class User(
    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = true)
    var password: String?,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.USER,

    @Column(unique = true, nullable = true)
    var username: String? = null,

    @Column(name = "phone_number", unique = true, nullable = true)
    var phoneNumber: String? = null,

    @Column(name = "birth_date", nullable = true)
    var birthDate: LocalDate? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()

enum class UserRole { USER, ADMIN }
