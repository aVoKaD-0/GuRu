package com.ruege.mobile.data.network.dto.response

import com.ruege.mobile.data.local.entity.NewsEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для получения новостей от API.
 */
@JsonClass(generateAdapter = true)
data class NewsDto(
    @Json(name = "news_id") val newsId: Int,
    @Json(name = "title") val title: String,
    @Json(name = "publication_date") val publicationDate: String?,
    @Json(name = "content") val content: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "image_url") val imageUrl: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
) {
    /**
     * Конвертирует DTO в Entity для сохранения в БД.
     */
    fun toEntity(): NewsEntity {
        // Преобразуем строковые даты в миллисекунды
        val pubDate = parseISODate(publicationDate)
        val created = parseISODate(createdAt)
        val updated = parseISODate(updatedAt)
        
        val fullImageUrl = if (imageUrl?.startsWith("/") == true) {
            "http://46.8.232.191:80${imageUrl}"
        } else {
            imageUrl ?: "placeholder_image"
        }
        
        return NewsEntity(
            title,
            pubDate,
            description ?: content ?: "",
            fullImageUrl,
            created,
            updated
        )
    }
    
    /**
     * Парсит ISO строку с датой в миллисекунды
     */
    private fun parseISODate(dateStr: String?): Long {
        return try {
            // Если строка пустая - используем текущее время
            if (dateStr.isNullOrEmpty()) {
                System.currentTimeMillis()
            } else {
                // Пытаемся распарсить ISO дату
                java.time.Instant.parse(dateStr).toEpochMilli()
            }
        } catch (e: Exception) {
            System.currentTimeMillis() // В случае ошибки используем текущее время
        }
    }
} 