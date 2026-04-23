package com.example.commerce.user.entity

import com.example.commerce.common.BaseEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "social_accounts",
    uniqueConstraints = [UniqueConstraint(columnNames = ["provider", "provider_id"])],
)
class SocialAccount(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val provider: OAuthProvider,

    @Column(name = "provider_id", nullable = false)
    val providerId: String,

    @Column
    val providerEmail: String?,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
) : BaseEntity()

enum class OAuthProvider { GOOGLE, KAKAO, NAVER }
