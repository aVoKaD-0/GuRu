package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для PracticeStatisticsEntity при синхронизации.
 */
@JsonClass(generateAdapter = true)
data class PracticeStatisticSyncDto(
    @field:Json(name = "ege_number")
    val egeNumber: String,
    @field:Json(name = "total_attempts")
    val totalAttempts: Int,
    @field:Json(name = "correct_attempts")
    val correctAttempts: Int,
    @field:Json(name = "last_attempt_date")
    val lastAttemptDate: Long
) 