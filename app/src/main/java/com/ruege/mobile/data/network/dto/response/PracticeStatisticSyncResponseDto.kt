package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для PracticeStatisticsEntity при получении данных с сервера.
 */
@JsonClass(generateAdapter = true)
data class PracticeStatisticSyncResponseDto(
    @field:Json(name = "ege_number")
    val egeNumber: String,
    @field:Json(name = "total_attempts")
    val totalAttempts: Int,
    @field:Json(name = "correct_attempts")
    val correctAttempts: Int,
    @field:Json(name = "last_attempt_date")
    val lastAttemptDate: Long
) 