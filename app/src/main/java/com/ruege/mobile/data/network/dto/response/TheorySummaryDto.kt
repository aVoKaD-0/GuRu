package com.ruege.mobile.data.network.dto.response

import com.ruege.mobile.data.local.entity.ContentEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import timber.log.Timber

/**
 * DTO для краткой информации о теме теории.
 */
@JsonClass(generateAdapter = true)
data class TheorySummaryDto(
    @Json(name = "id") val id: Int,
    @Json(name = "ege_number") val egeNumber: Int,
    @Json(name = "title") val title: String,
    @Json(name = "type") val type: String? = null,
    @Json(name = "content_id") val contentId: String? = null
) {
    /**
     * Конвертирует DTO в ContentEntity для сохранения в БД.
     */
    fun toContentEntity(): ContentEntity {
        try {
            val safeType = type?.takeIf { it.isNotBlank() } ?: "theory"
            val safeContentId = contentId?.takeIf { it.isNotBlank() } ?: id.toString()
            val safeTitle = title.takeIf { it.isNotBlank() } ?: "Тема $egeNumber"
            
            Timber.d("TheorySummaryDto", "Converting to ContentEntity: id=$id, ege=$egeNumber, " +
                "type=${type ?: "NULL"} -> $safeType, contentId=${contentId ?: "NULL"} -> $safeContentId")
            
            val entity = ContentEntity(
                safeContentId, 
                safeTitle,
                null, 
                safeType, 
                null, 
                false,
                false,
                egeNumber,
                null 
            )
            
            Timber.d("TheorySummaryDto", "Created ContentEntity - contentId: ${entity.contentId}, " +
                  "title: ${entity.title}, type: ${entity.type}, all fields non-null? " +
                  "${entity.contentId != null && entity.title != null && entity.type != null}")
            
            return entity
        } catch (e: Exception) {
            Timber.e("TheorySummaryDto", "Error creating ContentEntity", e)
            return ContentEntity(
                id.toString(), 
                "Тема $egeNumber", 
                null, 
                "theory", 
                null, 
                false, 
                false, 
                egeNumber, 
                null
            )
        }
    }
} 