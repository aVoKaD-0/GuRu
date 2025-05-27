package com.ruege.mobile.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * DTO для PracticeAttemptEntity при получении данных с сервера.
 */
data class PracticeAttemptSyncResponseDto(
    @SerializedName("attempt_id_local") 
    val attemptIdLocal: Long? = null,
    @SerializedName("task_id")
    val taskId: Int,
    @SerializedName("is_correct")
    val isCorrect: Boolean,
    @SerializedName("attempt_date")
    val attemptDate: Long
) 