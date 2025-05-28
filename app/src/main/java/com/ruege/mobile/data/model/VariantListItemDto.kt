package com.ruege.mobile.data.model

import com.squareup.moshi.Json

data class VariantListItemDto(
    @Json(name = "variant_id")
    val variantId: Int,
    @Json(name = "name")
    val name: String?,
    @Json(name = "description")
    val description: String?,
    @Json(name = "is_official")
    val isOfficial: Boolean?,
    @Json(name = "created_at")
    val createdAt: String?,
    @Json(name = "updated_at")
    val updatedAt: String?,
    @Json(name = "task_count")
    val taskCount: Int?
) 