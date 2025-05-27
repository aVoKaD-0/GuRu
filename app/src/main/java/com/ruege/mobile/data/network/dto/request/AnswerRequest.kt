package com.ruege.mobile.data.network.dto.request

import com.google.gson.annotations.SerializedName

/**
 * DTO для отправки ответа пользователя на проверку.
 */
data class AnswerRequest(
    @SerializedName("user_answer") 
    val userAnswer: String
) 