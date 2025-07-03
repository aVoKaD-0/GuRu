package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponseWrapper(
    @Json(name = "user") val user: AuthResponseDto.UserDto?,
    @Json(name = "access_token") val accessToken: String?,
    @Json(name = "refresh_token") val refreshToken: String?,
    @Json(name = "message") val message: String?,
    @Json(name = "login_session_token") val loginSessionToken: String?,
    @Json(name = "two_factor_enable") val twoFactorEnable: Boolean?
) {
    fun toAuthResponseDto(): AuthResponseDto? {
        if (user != null && accessToken != null && refreshToken != null) {
            return AuthResponseDto(user = user, accessToken = accessToken, refreshToken = refreshToken, tokenType = "bearer")
        }
        return null
    }

    fun toLogin2faResponseDto(): Login2faResponseDto? {
        if (message != null && loginSessionToken != null) {
            return Login2faResponseDto(message, loginSessionToken, twoFactorEnable ?: true)
        }
        return null
    }
} 