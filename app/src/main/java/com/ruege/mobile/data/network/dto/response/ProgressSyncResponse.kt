package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для получения ответа о синхронизации прогресса от сервера.
 */
@JsonClass(generateAdapter = true)
data class ProgressSyncResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "content_id") val contentId: String?,
    @Json(name = "timestamp") val timestamp: String?
) 