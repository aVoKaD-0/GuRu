package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VerifyTfaRequestDto(
    @Json(name = "login_session_token") val loginSessionToken: String,
    @Json(name = "code") val tfaCode: String
) 