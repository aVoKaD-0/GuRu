package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для запроса выхода из аккаунта.
 */
@JsonClass(generateAdapter = true)
data class LogoutRequestDto(
    @Json(name = "refresh_token") val refreshToken: String
) 