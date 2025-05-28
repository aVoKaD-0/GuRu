package com.ruege.mobile.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * DTO для ответа от сервера при синхронизации данных практики.
 */
data class PracticeSyncResponse(
    @SerializedName("message") 
    val message: String? = null,
    @SerializedName("statistics")
    val statistics: List<PracticeStatisticSyncResponseDto>,
    @SerializedName("attempts")
    val attempts: List<PracticeAttemptSyncResponseDto>
) 