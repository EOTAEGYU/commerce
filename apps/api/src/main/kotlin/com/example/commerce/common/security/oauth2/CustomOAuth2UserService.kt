package com.example.commerce.common.security.oauth2

import com.example.commerce.common.CustomException
import com.example.commerce.common.ErrorCode
import com.example.commerce.user.entity.OAuthProvider
import com.example.commerce.user.entity.SocialAccount
import com.example.commerce.user.entity.User
import com.example.commerce.user.repository.SocialAccountRepository
import com.example.commerce.user.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
) : DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oauth2User = super.loadUser(userRequest)

        val registrationId = userRequest.clientRegistration.registrationId
        val provider = when (registrationId.lowercase()) {
            "google" -> OAuthProvider.GOOGLE
            "kakao" -> OAuthProvider.KAKAO
            "naver" -> OAuthProvider.NAVER
            else -> throw CustomException(ErrorCode.OAUTH_PROVIDER_ERROR)
        }

        val info: OAuth2UserInfo = when (provider) {
            OAuthProvider.GOOGLE -> GoogleOAuth2UserInfo(oauth2User.attributes)
            OAuthProvider.KAKAO -> KakaoOAuth2UserInfo(oauth2User.attributes)
            OAuthProvider.NAVER -> NaverOAuth2UserInfo(oauth2User.attributes)
        }

        val email = info.email ?: "${info.providerId}@${provider.name.lowercase()}.local"

        val user = userRepository.findByEmail(email)
            ?: userRepository.save(
                User(
                    email = email,
                    password = null,
                    name = info.name,
                )
            )

        if (socialAccountRepository.findByProviderAndProviderId(provider, info.providerId) == null) {
            socialAccountRepository.save(
                SocialAccount(
                    user = user,
                    provider = provider,
                    providerId = info.providerId,
                    providerEmail = info.email,
                )
            )
        }

        return DefaultOAuth2User(
            emptyList(),
            mapOf("userId" to user.id),
            "userId",
        )
    }
}
