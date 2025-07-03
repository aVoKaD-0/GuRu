package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PasswordRecoveryRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "recaptcha_token") val recaptchaToken: String
) 