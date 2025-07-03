package com.ruege.mobile.data.network.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Адаптер для Moshi, который конвертирует ISO строки дат в Long (миллисекунды) и обратно.
 * Используется для правильной десериализации дат из JSON-ответов от сервера.
 */
class DateAdapter {
    /**
     * Преобразует ISO строку даты в миллисекунды
     * @param dateString ISO строка даты
     * @return количество миллисекунд или null, если строка некорректна
     */
    @FromJson
    fun fromJson(dateString: String?): Long? {
        if (dateString == null) return null
        
        return try {
            Instant.parse(dateString).toEpochMilli()
        } catch (e: DateTimeParseException) {
            null
        }
    }
    
    /**
     * Преобразует миллисекунды в ISO строку даты
     * @param timestamp количество миллисекунд
     * @return ISO строка даты или null, если timestamp равен null
     */
    @ToJson
    fun toJson(timestamp: Long?): String? {
        if (timestamp == null) return null
        
        return try {
            DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(timestamp))
        } catch (e: Exception) {
            null
        }
    }
} 