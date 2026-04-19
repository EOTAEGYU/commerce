package com.example.commerce.user.dto

import com.example.commerce.user.entity.User

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
    val role: String,
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role.name,
        )
    }
}
