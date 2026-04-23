package com.example.commerce.user.repository

import com.example.commerce.user.entity.OAuthProvider
import com.example.commerce.user.entity.SocialAccount
import com.example.commerce.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface SocialAccountRepository : JpaRepository<SocialAccount, Long> {
    fun findByProviderAndProviderId(provider: OAuthProvider, providerId: String): SocialAccount?
    fun findByUserAndProvider(user: User, provider: OAuthProvider): SocialAccount?
}
