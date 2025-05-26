package com.ruege.mobile.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * DTO для PracticeAttemptEntity при синхронизации.
 */
data class PracticeAttemptSyncDto(
    @SerializedName("attempt_id_local") // Чтобы не конфликтовать с серверным ID, если он будет другим
    val attemptIdLocal: Long,
    @SerializedName("task_id")
    val taskId: Int,
    @SerializedName("is_correct")
    val isCorrect: Boolean,
    @SerializedName("attempt_date")
    val attemptDate: Long
) 