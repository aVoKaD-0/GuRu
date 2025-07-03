package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PracticeAttemptCreateDto(
    @field:Json(name = "task_id")
    val taskId: String,
    @field:Json(name = "is_correct")
    val isCorrect: Boolean,
    @field:Json(name = "attempt_date")
    val attemptDate: Long
) 