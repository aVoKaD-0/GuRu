package com.ruege.mobile.data.network.dto.response

import com.ruege.mobile.data.local.entity.TaskEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для получения заданий от API из таблицы ruege_tasks.
 */
@JsonClass(generateAdapter = true)
data class TaskDto(
    @Json(name = "id") val id: Int,
    @Json(name = "fipi_id") val fipiId: String?,
    @Json(name = "ege_number") val egeNumber: String,
    @Json(name = "task_text") val taskText: String,
    @Json(name = "solution") val solution: String?,
    @Json(name = "explanation") val explanation: String?,
    @Json(name = "source") val source: String?,
    @Json(name = "text_id") val textId: Int?,
    @Json(name = "text") val text: TextDataDto? = null
) {
    /**
     * Конвертирует DTO в Entity для сохранения в БД.
     */
    fun toEntity(): TaskEntity {
        return TaskEntity(
            this.id,
            this.fipiId,
            this.egeNumber,
            this.taskText,
            this.solution,
            this.explanation,
            this.source,
            this.textId,
            "TEXT" // По умолчанию используем тип TEXT
        )
    }
}

/**
 * DTO для вариантов ответа в задании.
 */
@JsonClass(generateAdapter = true)
data class TaskOptionDto(
    @Json(name = "option_id") val optionId: Int,
    @Json(name = "task_id") val taskId: Int,
    @Json(name = "text") val text: String,
    @Json(name = "is_correct") val isCorrect: Boolean
) 