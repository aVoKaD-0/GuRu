package com.ruege.mobile.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * DTO для запроса на синхронизацию данных практики.
 */
data class PracticeSyncRequest(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("statistics")
    val statistics: List<PracticeStatisticSyncDto>,
    @SerializedName("attempts")
    val attempts: List<PracticeAttemptSyncDto>
) 