package com.ruege.mobile.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VariantListItemDto(
    @Json(name = "variant_id")
    val variantId: Int,

    @Json(name = "name")
    val name: String,

    @Json(name = "description")
    val description: String?,

    @Json(name = "is_official")
    val isOfficial: Boolean,

    @Json(name = "task_count")
    val taskCount: Int,

    @Json(name = "created_at")
    val createdAt: String,
) 