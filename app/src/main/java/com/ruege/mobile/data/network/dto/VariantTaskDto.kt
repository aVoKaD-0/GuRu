package com.ruege.mobile.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VariantTaskDto(
    @Json(name = "variant_task_id")
    val id: Int,

    @Json(name = "variant_id")
    val variantId: Int,

    @Json(name = "order_in_variant")
    val orderInVariant: Int,

    @Json(name = "ege_number")
    val egeNumber: String?,

    @Json(name = "title")
    val title: String?,

    @Json(name = "task_statement")
    val taskStatement: String?,

    @Json(name = "task_type")
    val taskType: String?,

    @Json(name = "max_points")
    val maxPoints: Int,

    @Json(name = "difficulty")
    val difficulty: Int?,

    @Json(name = "options")
    val options: List<VariantTaskOptionDto>?,

    @Json(name = "original_task_id")
    val originalTaskId: Int?,

    @Json(name = "variant_shared_text_id")
    val variantSharedTextId: Int?,

    @Json(name = "solution_text")
    val solutionText: String?,

    @Json(name = "explanation_text")
    val explanationText: String?,

    @Json(name = "time_limit")
    val timeLimit: Int?,

    @Json(name = "created_at")
    val createdAt: String?,

    @Json(name = "updated_at")
    val updatedAt: String?,

    @Json(name = "user_variant_task_answers")
    val userVariantTaskAnswers: UserVariantTaskAnswerDto?
) 