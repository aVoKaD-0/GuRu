package com.ruege.mobile.utils

/**
 * Общий класс для представления состояния загрузки данных.
 */
sealed class Resource<T>(
    @JvmField val data: T? = null,
    @JvmField val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data = data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data = data, message = message)
    class Loading<T>(data: T? = null) : Resource<T>(data = data)
} 