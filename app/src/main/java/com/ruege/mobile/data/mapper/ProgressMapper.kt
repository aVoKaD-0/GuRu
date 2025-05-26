@file:JvmName("ProgressMapper")
package com.ruege.mobile.data.mapper

import android.util.Log
import com.ruege.mobile.data.local.entity.ProgressEntity
import com.ruege.mobile.data.network.dto.ProgressUpdateRequest
import com.ruege.mobile.data.network.dto.response.ProgressSyncItemDto
import org.json.JSONArray
import org.json.JSONException
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Расширение для преобразования DTO в сущность прогресса
 */
fun ProgressSyncItemDto.toProgressEntity(): ProgressEntity? {
    try {
        // Проверяем обязательные поля
        if (contentId == null || contentId.isBlank() || userId == null) {
            Log.e("ProgressMapper", "Ошибка: DTO имеет null или пустые обязательные поля")
            return null
        }
        
        // Создаем новую сущность и задаем базовые поля
        val entity = ProgressEntity()
        entity.setContentId(this.contentId)
        entity.setPercentage(this.percentage)
        entity.setCompleted(this.completed)
        entity.setUserId(this.userId)

        // Определяем lastAccessed - используем либо lastAccessed из DTO, либо timestamp, либо текущее время
        val lastAccessedTimestamp: Long = if (!this.lastAccessed.isNullOrEmpty()) {
            try {
                OffsetDateTime.parse(this.lastAccessed).toInstant().toEpochMilli()
            } catch (e: DateTimeParseException) {
                Log.e("ProgressMapper", "Ошибка при парсинге даты lastAccessed: ${this.lastAccessed}", e)
                this.timestamp ?: System.currentTimeMillis() // Fallback на timestamp DTO или текущее время
            }
        } else {
            this.timestamp ?: System.currentTimeMillis() // Если lastAccessed пуст или null, используем timestamp DTO или текущее время
        }
        entity.setLastAccessed(lastAccessedTimestamp)
        
        // Определяем заголовок из contentId
        if (contentId.startsWith("task_group_")) {
            val taskNumber = contentId.replace("task_group_", "")
            entity.setTitle("Задание $taskNumber")
        } else {
            // Если не task_group_, используем какой-то другой подход для заголовка
            entity.setTitle(this.contentId) // или null, или другая логика
        }
        
        // Устанавливаем пустое описание (может быть заполнено позже)
        entity.setDescription("")
        
        // Конвертируем список решенных заданий в JSON, если он есть
        if (this.solvedTaskIds != null && this.solvedTaskIds.isNotEmpty()) {
            entity.setSolvedTaskIds(ProgressEntity.listToJsonString(this.solvedTaskIds))
        } else {
            entity.setSolvedTaskIds("[]")
        }
        
        return entity
    } catch (e: Exception) {
        Log.e("ProgressMapper", "Ошибка при создании сущности из DTO", e)
        return null
    }
}

/**
 * Преобразует сущность прогресса в DTO для обновления на сервере
 */
fun toProgressUpdateDto(progressEntity: ProgressEntity): ProgressUpdateRequest {
    // Используем уже распарсенный список из сущности вместо повторного парсинга
    val solvedTaskIds = progressEntity.getSolvedTaskIdsList().takeIf { it.isNotEmpty() }
    
    return ProgressUpdateRequest(
        contentId = progressEntity.getContentId(),
        percentage = progressEntity.getPercentage(),
        completed = progressEntity.isCompleted(),
        timestamp = progressEntity.getLastAccessed(),
        solvedTaskIds = solvedTaskIds
    )
}

/**
 * Преобразует JSON-строку с решенными заданиями в список строк
 * Используется только для элементов из очереди синхронизации
 */
fun parseJsonSolvedTaskIds(jsonStr: String?): List<String>? {
    if (jsonStr == null || jsonStr.isEmpty() || jsonStr == "[]") {
        return null
    }
    
    return try {
        val jsonArray = JSONArray(jsonStr)
        val solvedIds = ArrayList<String>(jsonArray.length())
        for (i in 0 until jsonArray.length()) {
            solvedIds.add(jsonArray.getString(i))
        }
        solvedIds.takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        Log.e("ProgressMapper", "Ошибка при парсинге списка решенных заданий: $jsonStr", e)
        null
    }
} 