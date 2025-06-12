package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для ответа от сервера при GET запросе на синхронизацию статистики.
 */
@JsonClass(generateAdapter = true)
data class PracticeStatisticsGetResponse(
    @field:Json(name = "user_id")
    val userId: String,
    @field:Json(name = "last_sync_timestamp")
    val lastSyncTimestamp: Long,
    @field:Json(name = "aggregated_statistics")
    val statistics: List<PracticeStatisticSyncResponseDto>,
    @field:Json(name = "recent_attempts")
    val attempts: List<PracticeAttemptSyncResponseDto>
) 