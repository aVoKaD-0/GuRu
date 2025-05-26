package com.ruege.mobile.data.network.dto.response

import com.google.gson.annotations.SerializedName

/**
 * DTO для PracticeAttemptEntity при получении данных с сервера.
 */
data class PracticeAttemptSyncResponseDto(
    // Если сервер генерирует свой ID для попытки, можно его добавить сюда.
    // @SerializedName("server_attempt_id")
    // val serverAttemptId: Long,

    @SerializedName("attempt_id_local") // Локальный ID, чтобы сопоставить с существующей записью
    val attemptIdLocal: Long? = null, // Может быть null, если это новая запись с сервера
    @SerializedName("task_id")
    val taskId: Int,
    @SerializedName("is_correct")
    val isCorrect: Boolean,
    @SerializedName("attempt_date")
    val attemptDate: Long
) 