package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EssayCheckResult(
    @Json(name = "status") val status: String,
    @Json(name = "result") val result: String?,
    @Json(name = "essay_content") val essayContent: String?,
    @Json(name = "detail") val detail: String?
) 