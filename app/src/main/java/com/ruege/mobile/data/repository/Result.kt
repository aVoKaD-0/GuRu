package com.ruege.mobile.data.repository;

/**
 * Общий класс для представления состояний операций: Загрузка, Успех, Ошибка.
 */
sealed class Result<out T> {
    object Loading : Result<Nothing>()
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[message=$message]"
            is Loading -> "Loading"
        }
    }
} 