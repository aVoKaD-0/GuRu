package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SetUsernameRequestDto(
    @Json(name = "session_token") val session_token: String,
    @Json(name = "username") val username: String
)
