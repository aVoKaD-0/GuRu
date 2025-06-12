package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для PracticeAttemptEntity при синхронизации.
 */
@JsonClass(generateAdapter = true)
data class PracticeAttemptSyncDto(
    @field:Json(name = "attempt_id_local")
    val attemptIdLocal: Long,
    @field:Json(name = "task_id")
    val taskId: Int,
    @field:Json(name = "is_correct")
    val isCorrect: Boolean,
    @field:Json(name = "attempt_date")
    val attemptDate: Long
) 