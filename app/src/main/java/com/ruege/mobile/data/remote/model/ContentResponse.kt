package com.ruege.mobile.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Модель ответа API для контента
 */
@JsonClass(generateAdapter = true)
data class ContentResponse(
    @Json(name = "id")
    val id: String = "",

    @Json(name = "title")
    val title: String = "",

    @Json(name = "description")
    val description: String = "",

    @Json(name = "type") 
    val type: String = "",

    @Json(name = "parent_id")
    val parentId: String? = null,

    @Json(name = "content_url")
    val contentUrl: String? = null,

    @Json(name = "order_position")
    val orderPosition: Int = 0,

    @Json(name = "ege_number")
    val egeNumber: Int = 0
) 