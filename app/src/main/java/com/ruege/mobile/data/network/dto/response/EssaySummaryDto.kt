package com.ruege.mobile.data.network.dto.response

import com.ruege.mobile.data.local.entity.ContentEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import android.util.Log

/**
 * DTO для краткой информации о теме сочинения.
 */
@JsonClass(generateAdapter = true)
data class EssaySummaryDto(
    @Json(name = "id") val id: Int,
    @Json(name = "ege_number") val egeNumber: Int,
    @Json(name = "title") val title: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "content_id") val contentId: String? = null
) {
    fun toContentEntity(): ContentEntity {
        try {
            val safeType = type?.takeIf { it.isNotBlank() } ?: "essay"
            val safeContentId = contentId?.takeIf { it.isNotBlank() } ?: id.toString()
            val safeTitle = title.takeIf { it.isNotBlank() } ?: "Сочинение $id" 

            Log.d("EssaySummaryDto", "Converting to ContentEntity: id=$id, ege=$egeNumber, " +
                "type=${type ?: "NULL"} -> $safeType, contentId=${contentId ?: "NULL"} -> $safeContentId")

            val entity = ContentEntity(
                safeContentId,
                safeTitle,
                null, 
                safeType,
                null, 
                false,
                false,
                id, 
                null 
            )

            Log.d("EssaySummaryDto", "Created ContentEntity - contentId: ${entity.contentId}, " +
                  "title: ${entity.title}, type: ${entity.type}")

            return entity
        } catch (e: Exception) {
            Log.e("EssaySummaryDto", "Error creating ContentEntity", e)
            return ContentEntity(
                id.toString(),
                "Сочинение $id",
                null,
                "essay",
                null,
                false,
                false,
                id,
                null
            )
        }
    }
} 