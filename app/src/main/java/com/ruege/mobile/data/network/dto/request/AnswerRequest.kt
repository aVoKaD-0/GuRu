package com.ruege.mobile.data.network.dto.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для отправки ответа пользователя на проверку.
 */
@JsonClass(generateAdapter = true)
data class AnswerRequest(
    @field:Json(name = "user_answer")
    val userAnswer: String
) 