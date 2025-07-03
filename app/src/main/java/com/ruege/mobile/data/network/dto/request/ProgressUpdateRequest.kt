package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для отправки обновлений прогресса на сервер.
 * @property contentId Идентификатор контента
 * @property percentage Процент выполнения
 * @property completed Флаг завершения
 * @property timestamp Временная метка последнего обновления
 */
@JsonClass(generateAdapter = true)
data class ProgressUpdateRequest(
    @Json(name = "content_id") val contentId: String,
    @Json(name = "percentage") val percentage: Int,
    @Json(name = "completed") val completed: Boolean,
    @Json(name = "timestamp") val timestamp: Long
) 