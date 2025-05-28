package com.ruege.mobile.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для запроса на обновление прогресса
 */
@JsonClass(generateAdapter = true)
data class ProgressUpdateRequest(
    @Json(name = "content_id") 
    val contentId: String,
    
    @Json(name = "percentage") 
    val percentage: Int,
    
    @Json(name = "completed") 
    val completed: Boolean,
    
    @Json(name = "timestamp") 
    val timestamp: Long,
    
    @Json(name = "solved_task_ids") 
    val solvedTaskIds: List<String>? = null
)

/**
 * DTO для ответа на запрос обновления прогресса
 */
@JsonClass(generateAdapter = true)
data class ProgressSyncResponse(
    @Json(name = "success") 
    val success: Boolean,
    
    @Json(name = "message") 
    val message: String? = null,
    
    @Json(name = "content_id") 
    val contentId: String? = null,
    
    @Json(name = "timestamp") 
    val timestamp: String? = null
) 