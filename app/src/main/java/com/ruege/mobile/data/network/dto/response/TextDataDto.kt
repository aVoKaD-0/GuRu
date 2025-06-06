package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TextDataDto(
    @Json(name = "id")
    val id: Int?,
    @Json(name = "content")
    val content: String?,
    @Json(name = "author")
    val author: String?,
    @Json(name = "source")
    val source: String?,
    @Json(name = "title")
    val title: String?
) 