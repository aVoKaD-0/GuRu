package com.ruege.mobile.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных задания для отображения в UI.
 */
@Parcelize
data class TaskItem(
    val taskId: String,
    val title: String, // Используется для заголовка типа "Задание X"
    val egeTaskNumber: String, // Номер задания в ЕГЭ (соответствует TaskEntity.egeNumber)
    val description: String, // Может использоваться для общего описания, не в деталях задания
    val content: String,  // HTML-разметка с самим заданием
    val answerType: AnswerType, 
    val maxPoints: Int,
    val timeLimit: Int, // Не используется в BottomSheet в текущей реализации
    val solutions: List<Solution>? = null,  // Варианты ответов для тестов (если AnswerType это SINGLE_CHOICE/MULTIPLE_CHOICE)
    var correctAnswer: String? = null, // Правильный ответ для текстовых/числовых заданий
    var explanation: String? = null,   // Объяснение к заданию
    val textId: Int? = null, // ID связанного текста, если он есть и должен грузиться ОТДЕЛЬНО

    // Поля состояния ответа пользователя (добавлены для BottomSheetDialogFragment)
    var userAnswer: String? = null,
    var isSolved: Boolean = false, 
    var isCorrect: Boolean? = null,
    var scoreAchieved: Int? = null,
    var attemptsMade: Int = 0 // Для логики нескольких попыток, если нужно
) : Parcelable

/**
 * Типы ответов на задания.
 */
@Parcelize
enum class AnswerType : Parcelable {
    TEXT,  // Свободный ввод текстового ответа
    SINGLE_CHOICE,  // Один вариант из предложенных
    MULTIPLE_CHOICE,  // Несколько вариантов из предложенных
    NUMBER  // Числовой ответ
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
 * Этот класс может быть не нужен, если TaskItem сам хранит состояние.
 * Но он может использоваться ViewModel для получения ответа от Repository.
 */
@Parcelize
data class AnswerCheckResult(
    val taskId: String, // Чтобы знать, к какому заданию относится результат
    val isCorrect: Boolean,
    val explanation: String?, 
    val correctAnswer: String?,
    val userAnswer: String,
    val pointsAwarded: Int
) : Parcelable 