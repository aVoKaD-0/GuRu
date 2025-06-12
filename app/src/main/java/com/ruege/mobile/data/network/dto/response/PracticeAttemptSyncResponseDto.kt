package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для PracticeAttemptEntity при получении данных с сервера.
 */
@JsonClass(generateAdapter = true)
data class PracticeAttemptSyncResponseDto(
    @field:Json(name = "attempt_id_local")
    val attemptIdLocal: Long? = null,
    @field:Json(name = "task_id")
    val taskId: Int,
    @field:Json(name = "is_correct")
    val isCorrect: Boolean,
    @field:Json(name = "attempt_date")
    val attemptDate: Long
) 