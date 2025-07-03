package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterStartResponseDto(
    @Json(name = "session_token") val session_token: String,
    @Json(name = "message") val message: String
)
