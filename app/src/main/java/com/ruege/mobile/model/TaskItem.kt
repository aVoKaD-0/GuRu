package com.ruege.mobile.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных задания для отображения в UI.
 */
@Parcelize
data class TaskItem(
    val taskId: String,
    val title: String, 
    val egeTaskNumber: String,
    val description: String, 
    val content: String,  
    val answerType: AnswerType, 
    val maxPoints: Int,
    val timeLimit: Int, 
    val solutions: List<Solution>? = null,
    var correctAnswer: String? = null,
    var explanation: String? = null,  
    val textId: Int? = null,
    val orderPosition: Int = 0,  // Добавлено поле для сортировки заданий

    var userAnswer: String? = null,
    var isSolved: Boolean = false, 
    var isCorrect: Boolean? = null,
    var scoreAchieved: Int? = null,
    var attemptsMade: Int = 0  
) : Parcelable

/**
 * Типы ответов на задания.
 */
@Parcelize
enum class AnswerType : Parcelable {
    TEXT,  
    SINGLE_CHOICE,  
    MULTIPLE_CHOICE,
    NUMBER  
}

/**
 * Модель для вариантов ответа.
 */
@Parcelize
data class Solution(
    val id: String,
    val text: String,
    val isCorrect: Boolean = false
) : Parcelable

/**
 * Результат проверки ответа пользователя.
 */
@Parcelize
data class AnswerCheckResult(
    val taskId: String, 
    val isCorrect: Boolean,
    val explanation: String?, 
    val correctAnswer: String?,
    val userAnswer: String,
    val pointsAwarded: Int
) : Parcelable 