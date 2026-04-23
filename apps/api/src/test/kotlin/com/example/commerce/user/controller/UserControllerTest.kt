package com.example.commerce.user.controller

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.common.security.JwtProvider
import com.example.commerce.common.security.oauth2.CustomOAuth2UserService
import com.example.commerce.common.security.oauth2.OAuth2FailureHandler
import com.example.commerce.common.security.oauth2.OAuth2SuccessHandler
import com.example.commerce.user.dto.AuthResponse
import com.example.commerce.user.dto.SignInRequest
import com.example.commerce.user.dto.SignUpRequest
import com.example.commerce.user.dto.UpdateProfileRequest
import com.example.commerce.user.dto.UserResponse
import com.example.commerce.user.service.UserService
import com.example.commerce.common.security.SecurityConfig
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

@WebMvcTest(UserController::class)
@Import(SecurityConfig::class)
class UserControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @MockitoBean lateinit var userService: UserService
    @MockitoBean lateinit var jwtProvider: JwtProvider
    @MockitoBean lateinit var customOAuth2UserService: CustomOAuth2UserService
    @MockitoBean lateinit var oauth2SuccessHandler: OAuth2SuccessHandler
    @MockitoBean lateinit var oauth2FailureHandler: OAuth2FailureHandler

    private val userResponse = UserResponse(
        id = 1L,
        email = "test@test.com",
        name = "홍길동",
        role = "USER",
        username = "hong1234",
        phoneNumber = "010-1234-5678",
        birthDate = LocalDate.of(1990, 1, 1),
    )
    private val authResponse = AuthResponse(accessToken = "jwt.token.value")

    private fun auth() = authentication(
        UsernamePasswordAuthenticationToken(1L, null, listOf(SimpleGrantedAuthority("ROLE_USER")))
    )

    @Nested
    inner class SignUp {

        @Test
        fun `정상 회원가입 시 201 반환`() {
            val request = SignUpRequest(
                email = "test@test.com",
                password = "password1!",
                name = "홍길동",
                username = "hong1234",
                phoneNumber = "010-1234-5678",
                birthDate = LocalDate.of(1990, 1, 1),
            )
            given(userService.signUp(any())).willReturn(userResponse)

            mockMvc.post("/api/users/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isCreated() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.email") { value("test@test.com") }
            }
        }

        @Test
        fun `이메일 형식 오류 시 400 반환`() {
            val request = SignUpRequest(
                email = "invalid-email",
                password = "password1!",
                name = "홍길동",
                username = "hong1234",
                phoneNumber = "010-1234-5678",
                birthDate = LocalDate.of(1990, 1, 1),
            )

            mockMvc.post("/api/users/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.success") { value(false) }
            }
        }

        @Test
        fun `비밀번호 8자 미만 시 400 반환`() {
            val request = SignUpRequest(
                email = "test@test.com",
                password = "short",
                name = "홍길동",
                username = "hong1234",
                phoneNumber = "010-1234-5678",
                birthDate = LocalDate.of(1990, 1, 1),
            )

            mockMvc.post("/api/users/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.success") { value(false) }
            }
        }

        @Test
        fun `이메일 중복 시 409 반환`() {
            val request = SignUpRequest(
                email = "test@test.com",
                password = "password1!",
                name = "홍길동",
                username = "hong1234",
                phoneNumber = "010-1234-5678",
                birthDate = LocalDate.of(1990, 1, 1),
            )
            given(userService.signUp(any())).willThrow(CustomException(ErrorCode.DUPLICATE_EMAIL))

            mockMvc.post("/api/users/signup") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isConflict() }
                jsonPath("$.error.code") { value("DUPLICATE_EMAIL") }
            }
        }
    }

    @Nested
    inner class SignIn {

        @Test
        fun `정상 로그인 시 200 및 토큰 반환`() {
            val request = SignInRequest(username = "testuser", password = "password1!")
            given(userService.signIn(any())).willReturn(authResponse)

            mockMvc.post("/api/users/signin") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.accessToken") { value("jwt.token.value") }
                jsonPath("$.data.tokenType") { value("Bearer") }
            }
        }

        @Test
        fun `잘못된 인증 정보로 로그인 시 401 반환`() {
            val request = SignInRequest(username = "testuser", password = "wrong_password")
            given(userService.signIn(any())).willThrow(CustomException(ErrorCode.INVALID_CREDENTIALS))

            mockMvc.post("/api/users/signin") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.error.code") { value("INVALID_CREDENTIALS") }
            }
        }
    }

    @Nested
    inner class GetMe {

        @Test
        fun `인증된 사용자 요청 시 200 및 프로필 반환`() {
            given(userService.getMe(1L)).willReturn(userResponse)

            mockMvc.get("/api/users/me") {
                with(auth())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.email") { value("test@test.com") }
            }
        }

        @Test
        fun `미인증 요청 시 401 반환`() {
            mockMvc.get("/api/users/me").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    inner class UpdateProfile {

        @Test
        fun `인증된 사용자 PUT 요청 시 200 및 수정된 프로필 반환`() {
            val request = UpdateProfileRequest(name = "김철수")
            val updatedResponse = UserResponse(
                id = 1L,
                email = "test@test.com",
                name = "김철수",
                role = "USER",
                username = "hong1234",
                phoneNumber = "010-1234-5678",
                birthDate = LocalDate.of(1990, 1, 1),
            )
            given(userService.updateProfile(any(), any())).willReturn(updatedResponse)

            mockMvc.put("/api/users/me") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(auth())
                with(csrf())
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.name") { value("김철수") }
            }
        }

        @Test
        fun `미인증 요청 시 401 반환`() {
            val request = UpdateProfileRequest(name = "김철수")

            mockMvc.put("/api/users/me") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(csrf())
            }.andExpect {
                status { isUnauthorized() }
            }
        }

        @Test
        fun `name 빈값 입력 시 400 반환`() {
            val request = UpdateProfileRequest(name = "")

            mockMvc.put("/api/users/me") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(request)
                with(auth())
                with(csrf())
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.success") { value(false) }
            }
        }
    }
}
