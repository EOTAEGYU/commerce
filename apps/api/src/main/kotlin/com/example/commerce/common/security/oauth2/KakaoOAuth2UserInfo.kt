package com.example.commerce.common.security.oauth2

import com.example.commerce.user.entity.OAuthProvider

class KakaoOAuth2UserInfo(
    private val attributes: Map<String, Any>,
) : OAuth2UserInfo {
    private val kakaoAccount: Map<*, *>
        get() = attributes["kakao_account"] as Map<*, *>

    private val profile: Map<*, *>
        get() = kakaoAccount["profile"] as Map<*, *>

    override val providerId: String get() = attributes["id"].toString()
    override val email: String? get() = kakaoAccount["email"] as String?
    override val name: String get() = profile["nickname"] as String
    override val provider: OAuthProvider get() = OAuthProvider.KAKAO
}
