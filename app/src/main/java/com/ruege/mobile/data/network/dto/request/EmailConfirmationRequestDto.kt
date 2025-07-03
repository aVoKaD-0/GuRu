package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для отправки кода подтверждения email.
 */
@JsonClass(generateAdapter = true)
data class EmailConfirmationRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "confirmation_code") val code: String
) 