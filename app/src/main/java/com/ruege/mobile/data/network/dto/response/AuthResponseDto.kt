package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для получения токенов от бэкенда.
 */
@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "user") val user: UserDto,
    @Json(name = "expires_in") val expiresIn: Int? = null
) {
    @JsonClass(generateAdapter = true)
    data class UserDto(
        @Json(name = "user_id") val userId: Int,
        @Json(name = "email") val email: String?,
        @Json(name = "username") val username: String?,
        @Json(name = "avatar_url") val avatarUrl: String?,
        @Json(name = "created_at") val createdAt: String,
        @Json(name = "last_login") val lastLogin: String?,
        @Json(name = "is_2fa_enabled") val is2faEnabled: Boolean = false
    )
} 