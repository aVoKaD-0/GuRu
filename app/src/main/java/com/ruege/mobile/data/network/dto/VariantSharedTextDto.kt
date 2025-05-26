package com.ruege.mobile.data.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VariantSharedTextDto(
    @Json(name = "variant_shared_text_id")
    val id: Int,

    @Json(name = "variant_id")
    val variantId: Int,

    @Json(name = "text_content")
    val textContent: String,

    @Json(name = "source_description")
    val sourceDescription: String,

    @Json(name = "created_at")
    val createdAt: String,

    @Json(name = "updated_at")
    val updatedAt: String
) 