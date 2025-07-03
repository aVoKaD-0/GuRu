package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Login2faResponseDto(
    @Json(name = "message") val message: String,
    @Json(name = "login_session_token") val loginSessionToken: String,
    @Json(name = "two_factor_enable") val twoFactorEnable: Boolean
) 