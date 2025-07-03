package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для запроса на вход по email и паролю.
 */
@JsonClass(generateAdapter = true)
data class EmailLoginRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "recaptcha_token") val recaptchaToken: String? = null
) 