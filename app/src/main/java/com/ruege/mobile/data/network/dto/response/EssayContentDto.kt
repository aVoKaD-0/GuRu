package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для полного содержимого сочинения.
 */
@JsonClass(generateAdapter = true)
data class EssayContentDto(
    @Json(name = "id") val id: Int,
    @Json(name = "ege_number") val egeNumber: Int, // Может быть нерелевантно для сочинений
    @Json(name = "title") val title: String,
    @Json(name = "content") val content: String, // HTML-содержимое сочинения
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "updated_at") val updatedAt: String
) 