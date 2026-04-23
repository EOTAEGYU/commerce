package com.example.commerce.common.security.oauth2

import com.example.commerce.user.entity.OAuthProvider

interface OAuth2UserInfo {
    val providerId: String
    val email: String?
    val name: String
    val provider: OAuthProvider
}
