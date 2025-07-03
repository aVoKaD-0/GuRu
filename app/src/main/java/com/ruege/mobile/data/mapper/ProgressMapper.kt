@file:JvmName("ProgressMapper")
package com.ruege.mobile.data.mapper

import timber.log.Timber
import com.ruege.mobile.data.local.entity.ProgressEntity
import com.ruege.mobile.data.network.dto.ProgressUpdateRequest
import com.ruege.mobile.data.network.dto.response.ProgressSyncItemDto
import org.json.JSONArray
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Расширение для преобразования DTO в сущность прогресса
 */
fun ProgressSyncItemDto.toProgressEntity(): ProgressEntity? {
    try {
        if (contentId == null || contentId.isBlank() || userId == null) {
            Timber.e("ProgressMapper", "Ошибка: DTO имеет null или пустые обязательные поля")
            return null
        }
        
        val entity = ProgressEntity()
        entity.setContentId(this.contentId)
        entity.setPercentage(this.percentage)
        entity.setCompleted(this.completed)
        entity.setUserId(this.userId)

        val lastAccessedTimestamp: Long = if (!this.lastAccessed.isNullOrEmpty()) {
            try {
                OffsetDateTime.parse(this.lastAccessed).toInstant().toEpochMilli()
            } catch (e: DateTimeParseException) {
                Timber.e("ProgressMapper", "Ошибка при парсинге даты lastAccessed: ${this.lastAccessed}", e)
                this.timestamp ?: System.currentTimeMillis()
            }
        } else {
            this.timestamp ?: System.currentTimeMillis()
        }
        entity.setLastAccessed(lastAccessedTimestamp)
        
        if (contentId.startsWith("task_group_")) {
            val taskNumber = contentId.replace("task_group_", "")
            entity.setTitle("Задание $taskNumber")
        } else {
            entity.setTitle(this.contentId)
        }
        
        entity.setDescription("")
        
        if (this.solvedTaskIds != null && this.solvedTaskIds.isNotEmpty()) {
            entity.setSolvedTaskIds(ProgressEntity.listToJsonString(this.solvedTaskIds))
        } else {
            entity.setSolvedTaskIds("[]")
        }
        
        return entity
    } catch (e: Exception) {
        Timber.e("ProgressMapper", "Ошибка при создании сущности из DTO", e)
        return null
    }
}

/**
 * Преобразует сущность прогресса в DTO для обновления на сервере
 */
fun toProgressUpdateDto(progressEntity: ProgressEntity): ProgressUpdateRequest {
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
        Timber.e("ProgressMapper", "Ошибка при парсинге списка решенных заданий: $jsonStr", e)
        null
    }
} 