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
import java.time.LocalDate
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
        username = "hong1234",
        phoneNumber = "010-1234-5678",
        birthDate = LocalDate.of(1990, 1, 1),
        id = id,
    )

    private fun signUpRequest() = SignUpRequest(
        email = "test@test.com",
        password = "password1!",
        name = "홍길동",
        username = "hong1234",
        phoneNumber = "010-1234-5678",
        birthDate = LocalDate.of(1990, 1, 1),
    )

    @Nested
    inner class SignUp {

        @Test
        fun `정상 회원가입 시 UserResponse 반환`() {
            val request = signUpRequest()
            val savedUser = createUser()

            every { userRepository.existsByEmail(request.email) } returns false
            every { userRepository.existsByUsername(request.username) } returns false
            every { userRepository.existsByPhoneNumber(request.phoneNumber) } returns false
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
            val request = signUpRequest()

            every { userRepository.existsByEmail(request.email) } returns true

            val exception = assertThrows<CustomException> { userService.signUp(request) }
            assertEquals(ErrorCode.DUPLICATE_EMAIL, exception.errorCode)
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `아이디 중복 시 DUPLICATE_USERNAME 예외 발생`() {
            val request = signUpRequest()

            every { userRepository.existsByEmail(request.email) } returns false
            every { userRepository.existsByUsername(request.username) } returns true

            val exception = assertThrows<CustomException> { userService.signUp(request) }
            assertEquals(ErrorCode.DUPLICATE_USERNAME, exception.errorCode)
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `전화번호 중복 시 DUPLICATE_PHONE 예외 발생`() {
            val request = signUpRequest()

            every { userRepository.existsByEmail(request.email) } returns false
            every { userRepository.existsByUsername(request.username) } returns false
            every { userRepository.existsByPhoneNumber(request.phoneNumber) } returns true

            val exception = assertThrows<CustomException> { userService.signUp(request) }
            assertEquals(ErrorCode.DUPLICATE_PHONE, exception.errorCode)
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `passwordEncoder가 null 반환 시 INTERNAL_SERVER_ERROR 예외 발생`() {
            val request = signUpRequest()

            every { userRepository.existsByEmail(request.email) } returns false
            every { userRepository.existsByUsername(request.username) } returns false
            every { userRepository.existsByPhoneNumber(request.phoneNumber) } returns false
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
            val request = SignInRequest(username = "testuser", password = "password1!")
            val user = createUser()
            val token = "jwt.token.value"

            every { userRepository.findByUsername(request.username) } returns user
            every { passwordEncoder.matches(request.password, "encoded_password") } returns true
            every { jwtProvider.generateToken(user) } returns token

            val result = userService.signIn(request)

            assertEquals(token, result.accessToken)
            assertEquals("Bearer", result.tokenType)
        }

        @Test
        fun `존재하지 않는 아이디로 로그인 시 INVALID_CREDENTIALS 예외 발생`() {
            val request = SignInRequest(username = "nouser", password = "password1!")

            every { userRepository.findByUsername(request.username) } returns null

            val exception = assertThrows<CustomException> { userService.signIn(request) }
            assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.errorCode)
        }

        @Test
        fun `비밀번호 불일치 시 INVALID_CREDENTIALS 예외 발생`() {
            val request = SignInRequest(username = "testuser", password = "wrong_password")
            val user = createUser()

            every { userRepository.findByUsername(request.username) } returns user
            every { passwordEncoder.matches(request.password, "encoded_password") } returns false

            val exception = assertThrows<CustomException> { userService.signIn(request) }
            assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.errorCode)
        }

        @Test
        fun `password가 null인 사용자 로그인 시 INVALID_CREDENTIALS 예외 발생`() {
            val request = SignInRequest(username = "oauthuser", password = "any_password")
            val oauthUser = User(
                email = "oauth@test.com",
                password = null,
                name = "OAuth 사용자",
            )

            every { userRepository.findByUsername(request.username) } returns oauthUser

            val exception = assertThrows<CustomException> { userService.signIn(request) }
            assertEquals(ErrorCode.INVALID_CREDENTIALS, exception.errorCode)
        }
    }

    @Nested
    inner class CheckUsernameDuplicate {

        @Test
        fun `사용 가능한 username 조회 시 available true 반환`() {
            every { userRepository.existsByUsername("newuser") } returns false

            val result = userService.checkUsernameDuplicate("newuser")

            assertEquals(true, result["available"])
        }

        @Test
        fun `이미 사용 중인 username 조회 시 available false 반환`() {
            every { userRepository.existsByUsername("hong1234") } returns true

            val result = userService.checkUsernameDuplicate("hong1234")

            assertEquals(false, result["available"])
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
        fun `username 변경 시 중복 검사 후 업데이트`() {
            val user = createUser(id = 1L)
            val request = UpdateProfileRequest(name = "홍길동", username = "newuser1")

            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userRepository.existsByUsername("newuser1") } returns false

            val result = userService.updateProfile(1L, request)

            assertEquals("newuser1", result.username)
        }

        @Test
        fun `username 중복 시 DUPLICATE_USERNAME 예외 발생`() {
            val user = createUser(id = 1L)
            val request = UpdateProfileRequest(name = "홍길동", username = "takenuser")

            every { userRepository.findById(1L) } returns Optional.of(user)
            every { userRepository.existsByUsername("takenuser") } returns true

            val exception = assertThrows<CustomException> { userService.updateProfile(1L, request) }
            assertEquals(ErrorCode.DUPLICATE_USERNAME, exception.errorCode)
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
