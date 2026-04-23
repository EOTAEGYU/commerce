package com.example.commerce.user.dto

import com.example.commerce.user.entity.User
import java.time.LocalDate

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: String,
    val username: String?,
    val phoneNumber: String?,
    val birthDate: LocalDate?,
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role.name,
            username = user.username,
            phoneNumber = user.phoneNumber,
            birthDate = user.birthDate,
        )
    }
}
