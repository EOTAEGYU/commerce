package com.example.commerce.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class SignUpRequest(
    @field:NotBlank @field:Email
    val email: String,

    @field:NotBlank @field:Size(min = 8)
    val password: String,

    @field:NotBlank
    val name: String,

    @field:NotBlank
    @field:Pattern(
        regexp = "^[a-z0-9]{4,16}$",
        message = "아이디는 영문 소문자와 숫자만 사용 가능하며 4~16자여야 합니다.",
    )
    val username: String,

    @field:NotBlank
    @field:Pattern(regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$")
    val phoneNumber: String,

    @field:NotNull
    @field:Past
    val birthDate: LocalDate,
)
