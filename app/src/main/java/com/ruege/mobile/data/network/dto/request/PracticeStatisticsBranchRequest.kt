package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PracticeStatisticsBranchRequest(
    @field:Json(name = "user_id")
    val userId: String,
    @field:Json(name = "last_known_server_sync_timestamp")
    val lastKnownServerSyncTimestamp: Long,
    @field:Json(name = "new_or_updated_aggregated_statistics")
    val newOrUpdatedAggregatedStatistics: List<PracticeStatisticSyncDto>,
    @field:Json(name = "new_attempts")
    val newAttempts: List<PracticeAttemptCreateDto>
) 