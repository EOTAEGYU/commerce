package com.example.commerce.common.security.oauth2

import com.example.commerce.user.entity.OAuthProvider

class NaverOAuth2UserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    override val providerId: String get() = attributes["id"] as String
    override val email: String? get() = attributes["email"] as String?
    override val name: String get() = attributes["name"] as String
    override val provider: OAuthProvider get() = OAuthProvider.NAVER
}
