package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для запроса на регистрацию по email и паролю.
 */
@JsonClass(generateAdapter = true)
data class EmailRegistrationRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
) 