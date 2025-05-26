package com.ruege.mobile.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * DTO для отправки ответа пользователя на проверку.
 */
data class AnswerRequest(
    @SerializedName("user_answer") // Имя поля должно совпадать с ожидаемым на бэкенде
    val userAnswer: String
    // Убираем старые поля: taskId, userId, spentTimeSeconds
) 