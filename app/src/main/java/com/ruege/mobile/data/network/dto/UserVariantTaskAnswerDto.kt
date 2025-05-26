package com.ruege.mobile.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserVariantTaskAnswerDto(
    @Json(name = "variant_task_option_id") // Это PK самого ответа на сервере
    val variantTaskOptionId: Int,

    @Json(name = "variant_task_id")
    val variantTaskId: Int,

    @Json(name = "variant_id")
    val variantId: Int,

    @Json(name = "user_id") // ID пользователя, который дал ответ (с сервера)
    val userId: Int,

    @Json(name = "user_submitted_answer") // Это алиас для option_text на сервере
    val userSubmittedAnswer: String,

    @Json(name = "is_correct")
    val isCorrect: Boolean,

    @Json(name = "explanation")
    val explanation: String?,

    @Json(name = "order_position") // Используется для маппинга в points_awarded
    val orderPosition: Int,

    @Json(name = "created_at")
    val createdAt: String,

    @Json(name = "updated_at") // Используется для маппинга в answered_timestamp
    val updatedAt: String
)
