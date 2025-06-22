package com.ruege.mobile.data.network.dto

import com.squareup.moshi.Json

/**
 * DTO для ответа от сервера при отправке ответов пользователя.
 * Соответствует схеме UserAnswerResponseItem на сервере.
 */
data class UserAnswerResponseItemDto(
    @Json(name = "variant_task_option_id")
    val variantTaskOptionId: Int,
    @Json(name = "variant_task_id")
    val variantTaskId: Int,
    @Json(name = "variant_id")
    val variantId: Int,
    @Json(name = "user_id")
    val userId: Int,
    @Json(name = "user_submitted_answer")
    val userSubmittedAnswer: String,
    @Json(name = "is_correct")
    val isCorrect: Boolean,
    @Json(name = "explanation")
    val explanation: String?,
    @Json(name = "order_position")
    val orderPosition: Int,
    @Json(name = "created_at")
    val createdAt: String, 
    @Json(name = "updated_at")
    val updatedAt: String
) 