package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "user_id")
    val userId: Int,
    @Json(name = "email")
    val email: String,
    @Json(name = "username")
    val username: String,
    @Json(name = "avatar_url")
    val avatarUrl: String?,
    @Json(name = "created_at")
    val createdAt: String,
    @Json(name = "last_login")
    val lastLogin: String?,
    @Json(name = "google_id")
    val googleId: String? = null
)
