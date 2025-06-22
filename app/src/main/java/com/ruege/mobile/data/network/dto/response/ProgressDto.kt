package com.ruege.mobile.data.network.dto.response

import com.ruege.mobile.data.local.entity.ProgressEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для получения прогресса пользователя от API.
 */
@JsonClass(generateAdapter = true)
data class ProgressDto(
    @Json(name = "progress_id") val progressId: Int,
    @Json(name = "user_id") val userId: Long,
    @Json(name = "category_id") val categoryId: Int,
    @Json(name = "completed_count") val completedCount: Int,
    @Json(name = "total_count") val totalCount: Int,
    @Json(name = "percentage") val percentage: Float,
    @Json(name = "last_updated") val lastUpdated: Long
) {
    fun toEntity(): ProgressEntity {
        val completed = completedCount == totalCount
        val lastAccessedLong: Long
        if (lastUpdated != null) {
            lastAccessedLong = lastUpdated
        } else {
            lastAccessedLong = System.currentTimeMillis()
        }
        return ProgressEntity(
            categoryId.toString(),
            percentage.toInt(),
            lastAccessedLong,
            completed,
            "Прогресс по категории $categoryId",
            userId
        )
    }
} 