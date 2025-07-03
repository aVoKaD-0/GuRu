package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EssayCheckAccepted(
    @Json(name = "check_id") val checkId: String,
    @Json(name = "message") val message: String
) 