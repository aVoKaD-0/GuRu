package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для отправки Google ID токена на бэкенд.
 */
@JsonClass(generateAdapter = true)
data class GoogleLoginRequestDto(
    @Json(name = "google_id_token") val googleIdToken: String
) 