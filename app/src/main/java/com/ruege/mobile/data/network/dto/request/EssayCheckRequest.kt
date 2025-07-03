package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EssayCheckRequest(
    @Json(name = "task_id") val taskId: Int? = null,
    @Json(name = "text_id") val textId: Int? = null,
    @Json(name = "variant_task_id") val variantTaskId: Int? = null,
    @Json(name = "essay_content") val text: String
) 