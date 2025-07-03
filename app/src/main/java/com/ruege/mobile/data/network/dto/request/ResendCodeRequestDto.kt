package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResendCodeRequestDto(
    @Json(name = "session_token") val sessionToken: String
) 