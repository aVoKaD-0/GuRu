package com.ruege.mobile.data.network.dto

import com.squareup.moshi.Json

data class UserAnswerPayloadDto(
    @Json(name = "variant_task_id")
    val variantTaskId: Int,
    @Json(name = "user_submitted_answer")
    val userSubmittedAnswer: String,
    @Json(name = "is_correct")
    val isCorrect: Boolean
) 