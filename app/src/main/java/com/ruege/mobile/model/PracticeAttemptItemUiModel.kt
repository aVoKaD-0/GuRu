package com.ruege.mobile.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PracticeAttemptItemUiModel(
    val attemptId: Long, // ID самой попытки, может пригодиться
    val taskId: Int,     // ID задачи
    val egeTaskNumberDisplay: String, // Например, "Задание 5" или "Задача ID: X" если egeNumber нет
    val attemptDateFormatted: String,
    val isCorrect: Boolean
    // Можно добавить поле для детального текста задания или ответа пользователя, если нужно
) : Parcelable 