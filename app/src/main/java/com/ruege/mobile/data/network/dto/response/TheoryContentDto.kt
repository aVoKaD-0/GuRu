package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для полного содержимого теории.
 */
@JsonClass(generateAdapter = true)
data class TheoryContentDto(
    @Json(name = "id") val id: Int,
    @Json(name = "ege_number") val egeNumber: Int,
    @Json(name = "title") val title: String,
    @Json(name = "content") val content: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
) 