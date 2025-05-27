package com.ruege.mobile.data.network.dto.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO для детальной информации о задании из таблицы ruege_tasks.
 */
@JsonClass(generateAdapter = true)
data class TaskDetailDto(
    @Json(name = "id") val id: Int,
    @Json(name = "fipi_id") val fipiId: String?,
    @Json(name = "ege_number") val egeNumber: String,
    @Json(name = "task_text") val taskText: String,
    @Json(name = "solution") val solution: String?,
    @Json(name = "explanation") val explanation: String?,
    @Json(name = "source") val source: String?,
    @Json(name = "text_id") val textId: Int?,
    @Json(name = "text") val text: TextDto?
) {
    val taskId: String get() = id.toString()
    val title: String get() = "Задание $egeNumber" 
    val description: String get() = if (text != null) "Прочитайте текст и выполните задание" else ""
    val content: String get() = if (text != null) "<div class='task-text'>${text.content}</div><div class='task-question'>$taskText</div>" else taskText
    val answerType: String get() = "TEXT"
    val maxPoints: Int get() = 10
    val timeLimit: Int get() = 60
    val solutions: List<SolutionDto>? get() = null

    fun toEntity(): com.ruege.mobile.data.local.entity.TaskEntity {
        return com.ruege.mobile.data.local.entity.TaskEntity(
            this.id,
            this.fipiId,
            this.egeNumber,
            this.taskText,
            this.solution,
            this.explanation,
            this.source,
            this.textId,
            "TEXT"
        )
    }
}

/**
 * DTO для текстового содержимого задания.
 */
@JsonClass(generateAdapter = true)
data class TextDto(
    @Json(name = "id") val id: Int,
    @Json(name = "content") val content: String,
    @Json(name = "author") val author: String?
)

/**
 * DTO для вариантов решения задания.
 */
@JsonClass(generateAdapter = true)
data class SolutionDto(
    @Json(name = "id") val id: String,
    @Json(name = "text") val text: String,
    @Json(name = "is_correct") val isCorrect: Boolean = false
) 