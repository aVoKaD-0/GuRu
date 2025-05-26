package com.ruege.mobile.data.repository // Или другой подходящий пакет, например com.ruege.mobile.util

/**
 * Общий класс для представления состояний операций: Загрузка, Успех, Ошибка.
 */
sealed class Result<out T : Any> {
    object Loading : Result<Nothing>()
    data class Success<out T : Any>(val data: T) : Result<T>()
    data class Failure(val exception: Exception) : Result<Nothing>()
} 