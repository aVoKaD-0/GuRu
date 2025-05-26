package com.ruege.mobile.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * DTO для ответа от сервера при синхронизации данных практики.
 */
data class PracticeSyncResponse(
    @SerializedName("message") // Опционально: сообщение от сервера (например, "Синхронизация успешна")
    val message: String? = null,
    @SerializedName("statistics")
    val statistics: List<PracticeStatisticSyncResponseDto>,
    @SerializedName("attempts")
    val attempts: List<PracticeAttemptSyncResponseDto>
    // Можно добавить поле last_sync_timestamp, если сервер его возвращает
    // @SerializedName("last_sync_timestamp")
    // val lastSyncTimestamp: Long?
) 