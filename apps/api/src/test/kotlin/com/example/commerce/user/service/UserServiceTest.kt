package com.example.commerce.user.service

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.common.security.JwtProvider
import com.example.commerce.user.dto.SignInRequest
import com.example.commerce.user.dto.SignUpRequest
import com.example.commerce.user.dto.UpdateProfileRequest
import com.example.commerce.user.entity.User
import com.example.commerce.user.entity.UserRole
import com.example.commerce.user.repository.UserRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

@ExtendWith(MockKExtension::class)
class UserServiceTest {

    @MockK lateinit var userRepository: UserRepository
    @MockK lateinit var passwordEncoder: PasswordEncoder
    @MockK lateinit var jwtProvider: JwtProvider

    @InjectMockKs
    lateinit var userService: UserService

    private fun createUser(id: Long = 1L) = User(
        email = "test@test.com",
        password = "encoded_password",
        name = "홍길동",
        role = UserRole.USER,
        id = id,
    )

    @Nested
    inner class SignUp {

        @Test
        fun `정상 회원가입 시 UserResponse 반환`() {
            val request = SignUpRequest(email = "test@test.com", password = "password1!", name = "홍길동")
            val savedUser = createUser()

            every { userRepository.existsByEmail(request.email) } returns false
            every { passwordEncoder.encode(request.password) } returns "encoded_password"
            every { userRepository.save(any()) } returns savedUser

            val result = userService.signUp(request)

            assertEquals(savedUser.id, result.id)
            assertEquals(savedUser.email, result.email)
            assertEquals(savedUser.name, result.name)
            verify(exactly = 1) { userRepository.save(any()) }
        }

        @Test
        fun `이메일 중복 시 DUPLICATE_EMAIL 예외 발생`() {
            val request = SignUpRequest(email = "test@test.com", password = "password1!", name = "홍길동")

            every { userRepository.existsByEmail(request.email) } returns true

            val exception = assertThrows<CustomException> { userService.signUp(request) }
            assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.errorCode)
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `passwordEncoder가 null 반환 시 INTERNAL_SERVER_ERROR 예외 발생`() {
            val request = SignUpRequest(email = "test@test.com", password = "password1!", name = "홍길동")

            every { userRepository.existsByEmail(request.email) } returns false
            every { passwordEncoder.encode(request.password) } returns null

            val exception = assertThrows<CustomException> { userService.signUp(request) }
            assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.errorCode)
            verify(exactly = 0) { userRepository.save(any()) }
        }
    }

    @Nested
    inner class SignIn {

        @Test
        fun `정상 로그인 시 AccessToken 반환`() {
            val request = SignInRequest(email = "test@test.com", password = "password1!")
            val user = createUser()
            val token = "jwt.token.value"

            every { userRepository.findByEmail(request.email) } returns user
            every { passwordEncoder.matches(request.password, user.password) } returns true
            every { jwtProvider.generateToken(user) } returns token

            val result = userService.signIn(request)

            assertEquals(token, result.accessToken)
            assertEquals("Bearer", result.tokenType)
        }

        @Test
        fun `존재하지 않는 이메일로 로그인 시 INVALID_CREDENTIALS 예외 발생`() {
            val request = SignInRequest(email = "none@test.com", password = "password1!")

            every { userRepository.findByEmail(request.email) } returns null

            val exception = assertThrows<CustomException> { userService.signIn(request) }
            assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.errorCode)
        }

        @Test
        fun `비밀번호 불일치 시 INVALID_CREDENTIALS 예외 발생`() {
            val request = SignInRequest(email = "test@test.com", password = "wrong_password")
            val user = createUser()

            every { userRepository.findByEmail(request.email) } returns user
            every { passwordEncoder.matches(request.password, user.password) } returns false

            val exception = assertThrows<CustomException> { userService.signIn(request) }
            assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.errorCode)
        }
    }

    @Nested
    inner class GetMe {

        @Test
        fun `유효한 userId로 조회 시 UserResponse 반환`() {
            val user = createUser(id = 1L)

            every { userRepository.findById(1L) } returns Optional.of(user)

            val result = userService.getMe(1L)

            assertEquals(user.id, result.id)
            assertEquals(user.email, result.email)
            assertEquals(user.name, result.name)
        }

        @Test
        fun `존재하지 않는 userId 조회 시 USER_NOT_FOUND 예외 발생`() {
            every { userRepository.findById(999L) } returns Optional.empty()

            val exception = assertThrows<CustomException> { userService.getMe(999L) }
            assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
        }
    }

    @Nested
    inner class UpdateProfile {

        @Test
        fun `정상 수정 시 변경된 name 반환`() {
            val user = createUser(id = 1L)
            val request = UpdateProfileRequest(name = "김철수")

            every { userRepository.findById(1L) } returns Optional.of(user)

            val result = userService.updateProfile(1L, request)

            assertEquals("김철수", result.name)
        }

        @Test
        fun `존재하지 않는 userId 수정 시 USER_NOT_FOUND 예외 발생`() {
            val request = UpdateProfileRequest(name = "김철수")

            every { userRepository.findById(999L) } returns Optional.empty()

            val exception = assertThrows<CustomException> { userService.updateProfile(999L, request) }
            assertEquals(ErrorCode.USER_NOT_FOUND, exception.errorCode)
        }
    }
}
