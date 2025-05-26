package com.ruege.mobile.model // Исправляем пакет

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PracticeStatisticItem(
    val id: String, // Может быть egeNumber
    val egeDisplayNumber: String, // Например, "Задание 1"
    val totalAttempts: Int,
    val correctAttempts: Int,
    val successRate: Int, // Уже рассчитанный процент
    val lastAttemptDateFormatted: String // Отформатированная дата для отображения
) : Parcelable 