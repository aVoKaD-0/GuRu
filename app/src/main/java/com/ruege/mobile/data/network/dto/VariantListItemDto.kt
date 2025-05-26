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
    
    // Поле updated_at отсутствует в списке, но есть в VariantEntity.
    // Если оно может приходить от сервера для списка, его нужно добавить.
    // Если оно только для деталей, то в этом DTO оно не нужно.
    // Пока оставляем без него, так как его нет в вашем примере /api/v1/variants/
) 