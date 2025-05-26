package com.ruege.mobile.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * DTO для PracticeStatisticsEntity при получении данных с сервера.
 */
data class PracticeStatisticSyncResponseDto(
    @SerializedName("ege_number")
    val egeNumber: String,
    @SerializedName("total_attempts")
    val totalAttempts: Int,
    @SerializedName("correct_attempts")
    val correctAttempts: Int,
    @SerializedName("last_attempt_date")
    val lastAttemptDate: Long
) 