package com.ruege.mobile.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VariantDetailDto(
    @Json(name = "variant_id")
    val variantId: Int,

    @Json(name = "name")
    val name: String,

    @Json(name = "description")
    val description: String?,

    @Json(name = "is_official")
    val isOfficial: Boolean,

    @Json(name = "created_at")
    val createdAt: String, // Оставляем как String, DateAdapter разберется

    @Json(name = "updated_at")
    val updatedAt: String?, // Оставляем как String, DateAdapter разберется

    @Json(name = "shared_texts")
    val sharedTexts: List<VariantSharedTextDto>?,

    @Json(name = "variant_tasks")
    val tasks: List<VariantTaskDto>
) 