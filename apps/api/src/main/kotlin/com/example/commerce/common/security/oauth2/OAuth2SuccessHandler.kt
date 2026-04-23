package com.example.commerce.common.security.oauth2

import com.example.commerce.common.security.JwtProvider
import com.example.commerce.user.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2SuccessHandler(
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    @Value("\${app.frontend-url:http://localhost:3000}") private val frontendUrl: String,
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val oauth2User = authentication.principal as DefaultOAuth2User
        val userId = oauth2User.attributes["userId"] as Long
        val user = userRepository.findById(userId).orElseThrow()
        val token = jwtProvider.generateToken(user)
        val redirectUrl = "$frontendUrl/oauth/callback?token=$token"
        redirectStrategy.sendRedirect(request, response, redirectUrl)
    }
}
