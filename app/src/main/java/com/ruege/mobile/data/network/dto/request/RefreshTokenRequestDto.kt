package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для запроса обновления токена.
 */
@JsonClass(generateAdapter = true)
data class RefreshTokenRequestDto(
    @Json(name = "refresh_token") val refreshToken: String
) 