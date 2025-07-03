package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для ответа с токенами аутентификации.
 */
@JsonClass(generateAdapter = true)
data class TokenDto(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "token_type") val tokenType: String = "bearer",
    @Json(name = "expires_in") val expiresIn: Int? = null
) 