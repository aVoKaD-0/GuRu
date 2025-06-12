package com.ruege.mobile.data.repository;

/**
 * Общий класс для представления состояний операций: Загрузка, Успех, Ошибка.
 */
sealed class Result<out T : Any> {
    object Loading : Result<Nothing>()
    data class Success<out T : Any>(val data: T) : Result<T>()
    data class Failure(val exception: Exception) : Result<Nothing>()
    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Failure -> "Error[exception=$exception]"
            is Loading -> "Loading"
        }
    }
} 