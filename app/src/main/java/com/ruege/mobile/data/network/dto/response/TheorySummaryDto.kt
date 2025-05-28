package com.ruege.mobile.data.network.dto.response

import com.ruege.mobile.data.local.entity.ContentEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import android.util.Log

/**
 * DTO для краткой информации о теме теории.
 */
@JsonClass(generateAdapter = true)
data class TheorySummaryDto(
    @Json(name = "id") val id: Int, // ID из БД
    @Json(name = "ege_number") val egeNumber: Int,
    @Json(name = "title") val title: String,
    @Json(name = "type") val type: String? = null, // Делаем поле nullable
    @Json(name = "content_id") val contentId: String? = null // Делаем поле nullable
) {
    /**
     * Конвертирует DTO в ContentEntity для сохранения в БД.
     */
    fun toContentEntity(): ContentEntity {
        try {
            // Используем значения из полей type и contentId если они есть, иначе используем локальные значения
            val safeType = type?.takeIf { it.isNotBlank() } ?: "theory"
            val safeContentId = contentId?.takeIf { it.isNotBlank() } ?: id.toString()
            val safeTitle = title.takeIf { it.isNotBlank() } ?: "Тема $egeNumber"
            
            Log.d("TheorySummaryDto", "Converting to ContentEntity: id=$id, ege=$egeNumber, " +
                "type=${type ?: "NULL"} -> $safeType, contentId=${contentId ?: "NULL"} -> $safeContentId")
            
            // Создаем ContentEntity с безопасными значениями
            val entity = ContentEntity(
                safeContentId, // Используем contentId из API или id как строку
                safeTitle,
                null, // description - пока нет
                safeType, // type из API или дефолтное "theory"
                null, // parentId - пока нет
                false, // isDownloaded
                false, // isNew
                egeNumber, // orderPosition - сортируем по номеру ЕГЭ
                null // contentUrl - пока нет
            )
            
            // Дополнительная проверка после создания объекта
            Log.d("TheorySummaryDto", "Created ContentEntity - contentId: ${entity.contentId}, " +
                  "title: ${entity.title}, type: ${entity.type}, all fields non-null? " +
                  "${entity.contentId != null && entity.title != null && entity.type != null}")
            
            return entity
        } catch (e: Exception) {
            Log.e("TheorySummaryDto", "Error creating ContentEntity", e)
            // Возвращаем fallback-объект с гарантированными значениями в случае ошибки
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