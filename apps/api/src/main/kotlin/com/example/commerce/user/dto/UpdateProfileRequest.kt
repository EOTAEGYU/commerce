package com.example.commerce.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import java.time.LocalDate

data class UpdateProfileRequest(
    @field:NotBlank val name: String,

    @field:Pattern(
        regexp = "^[a-z0-9]{4,16}$",
        message = "아이디는 영문 소문자와 숫자만 사용 가능하며 4~16자여야 합니다.",
    )
    val username: String? = null,

    @field:Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$")
    val phoneNumber: String? = null,

    val birthDate: LocalDate? = null,
)
