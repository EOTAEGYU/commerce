package com.example.commerce.user.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.common.security.JwtProvider
import com.example.commerce.user.dto.AuthResponse
import com.example.commerce.user.dto.SignInRequest
import com.example.commerce.user.dto.SignUpRequest
import com.example.commerce.user.dto.UserResponse
import com.example.commerce.user.entity.User
import com.example.commerce.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
) {
    fun signUp(request: SignUpRequest): UserResponse {
        if (userRepository.existsByEmail(request.email))
            throw CustomException(ErrorCode.DUPLICATE_EMAIL)

        val encodedPassword = passwordEncoder.encode(request.password)
            ?: throw CustomException(ErrorCode.INTERNAL_SERVER_ERROR)
        val user = User(
            email = request.email,
            password = encodedPassword,
            name = request.name,
        )
        return UserResponse.from(userRepository.save(user))
    }

    fun signIn(request: SignInRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw CustomException(ErrorCode.INVALID_CREDENTIALS)
        if (!passwordEncoder.matches(request.password, user.password))
            throw CustomException(ErrorCode.INVALID_CREDENTIALS)
        return AuthResponse(accessToken = jwtProvider.generateToken(user))
    }

    @Transactional(readOnly = true)
    fun getMe(userId: Long): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { CustomException(ErrorCode.USER_NOT_FOUND) }
        return UserResponse.from(user)
    }
}
