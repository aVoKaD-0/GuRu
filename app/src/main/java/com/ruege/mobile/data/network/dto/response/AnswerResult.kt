package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для результата проверки ответа.
 */
@JsonClass(generateAdapter = true)
data class AnswerResult(
    @Json(name = "is_correct") val isCorrect: Boolean,
    @Json(name = "explanation") val explanation: String,
    @Json(name = "correct_answer") val correctAnswer: String,
    @Json(name = "user_answer") val userAnswer: String,
    @Json(name = "points") val points: Int
) 