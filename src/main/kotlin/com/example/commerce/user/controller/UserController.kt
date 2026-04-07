package com.example.commerce.user.controller

import com.example.commerce.common.ApiResponse
import com.example.commerce.user.dto.AuthResponse
import com.example.commerce.user.dto.SignInRequest
import com.example.commerce.user.dto.SignUpRequest
import com.example.commerce.user.dto.UserResponse
import com.example.commerce.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {
    @PostMapping("/signup")
    fun signUp(@Valid @RequestBody request: SignUpRequest): ResponseEntity<ApiResponse<UserResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(userService.signUp(request)))

    @PostMapping("/signin")
    fun signIn(@Valid @RequestBody request: SignInRequest): ResponseEntity<ApiResponse<AuthResponse>> =
        ResponseEntity.ok(ApiResponse.success(userService.signIn(request)))

    @GetMapping("/me")
    fun getMe(@AuthenticationPrincipal userId: Long): ResponseEntity<ApiResponse<UserResponse>> =
        ResponseEntity.ok(ApiResponse.success(userService.getMe(userId)))
}
