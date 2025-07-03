package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VerifyRequestDto(
    @Json(name = "session_token") val session_token: String,
    @Json(name = "code") val code: String
)
