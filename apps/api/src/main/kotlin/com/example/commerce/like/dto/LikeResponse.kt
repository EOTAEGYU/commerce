package com.example.commerce.like.dto

data class LikeResponse(
    val productId: Long,
    val liked: Boolean,
    val likeCount: Long,
) {
    companion object {
        fun of(productId: Long, liked: Boolean, likeCount: Long) =
            LikeResponse(productId, liked, likeCount)
    }
}
