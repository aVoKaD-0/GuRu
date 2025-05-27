package com.ruege.mobile.data.network.dto.response // или куда вы складываете DTO для ответов

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProgressSyncItemDto(
    @Json(name = "content_id") val contentId: String,
    @Json(name = "percentage") val percentage: Int,
    @Json(name = "completed") val completed: Boolean,
    @Json(name = "timestamp") val timestamp: Long, 
    @Json(name = "progress_id") val progressId: Int,
    @Json(name = "user_id") val userId: Long,
    @Json(name = "created_at") val createdAt: String, 
    @Json(name = "updated_at") val updatedAt: String, 
    @Json(name = "last_accessed") val lastAccessed: String?, 
    @Json(name = "solved_task_ids") val solvedTaskIds: List<String>? = null 
)
