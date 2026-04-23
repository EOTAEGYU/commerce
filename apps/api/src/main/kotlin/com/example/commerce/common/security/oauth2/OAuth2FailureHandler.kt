package com.example.commerce.common.security.oauth2

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class OAuth2FailureHandler(
    @Value("\${app.frontend-url:http://localhost:3000}") private val frontendUrl: String,
) : SimpleUrlAuthenticationFailureHandler() {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException,
    ) {
        redirectStrategy.sendRedirect(request, response, "$frontendUrl/oauth/callback?error=oauth_failed")
    }
}
