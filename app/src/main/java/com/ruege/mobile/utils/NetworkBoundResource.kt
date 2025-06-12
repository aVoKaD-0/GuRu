package com.ruege.mobile.utilss

import android.util.Log
import kotlinx.coroutines.flow.*
import retrofit2.Response

/**
 * Функция-утилита для получения данных по принципу "сначала кэш, потом сеть".
 *
 * @param ResultType Тип данных, который мы хотим получить из БД (локальный кэш).
 * @param RequestType Тип данных, который мы получаем с сервера (DTO).
 * @param query Функция для получения данных из БД. Должна возвращать Flow.
 * @param fetch Функция для выполнения сетевого запроса. Должна быть suspend-функцией.
 * @param saveFetchResult Функция для сохранения результата сетевого запроса в БД. Должна быть suspend-функцией.
 * @param shouldFetch Лямбда, определяющая, нужно ли делать сетевой запрос. По умолчанию true.
 *                    Может принимать текущие данные из БД для принятия решения.
 * @param onFetchFailed Лямбда для обработки ошибок сети. По умолчанию просто логирует.
 * @param resourceName Имя ресурса для детализации логов.
 */
inline fun <ResultType, RequestType> networkBoundResource(
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend () -> Response<RequestType>,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: (ResultType?) -> Boolean = { true },
    crossinline onFetchFailed: (Throwable) -> Unit = { throwable ->
        Log.e("NBR_Error", "Default onFetchFailed: ${throwable.message}", throwable)
        throwable.printStackTrace()
    },
    resourceName: String = "UnknownResource"
) = channelFlow { 
    Log.d("NBR_$resourceName", "Starting for $resourceName")

    val data = query().firstOrNull()
    Log.d("NBR_$resourceName", "Initial data from cache: ${if (data != null) "present" else "null"}")
    send(Resource.Loading(data))

    val flow = query().map {
        Log.d("NBR_$resourceName", "Data from DB Flow: ${it?.toString()?.take(100)}")
        Resource.Success(it)
    }

    if (shouldFetch(data)) {
        Log.d("NBR_$resourceName", "shouldFetch is TRUE. Attempting network request.")
        try {
            Log.d("NBR_$resourceName", "Calling fetch()...")
            val response = fetch()
            Log.d("NBR_$resourceName", "fetch() completed. Response successful: ${response.isSuccessful}, Code: ${response.code()}")

            if (response.isSuccessful) {
                response.body()?.let { requestData ->
                    Log.d("NBR_$resourceName", "Response body is NOT null. Calling saveFetchResult...")
                    saveFetchResult(requestData)
                    Log.d("NBR_$resourceName", "saveFetchResult completed.")
                } ?: run {
                    Log.e("NBR_$resourceName", "Response body is NULL!")
                    val error = Exception("Response body is null for $resourceName")
                    onFetchFailed(error)
                    send(Resource.Error(error.message ?: "Response body is null", data))
                }
            } else {
                val errorMsgBody = response.errorBody()?.string() ?: "Unknown error from network (empty errorBody)"
                Log.e("NBR_$resourceName", "Network request failed. Code: ${response.code()}, Message: $errorMsgBody")
                val error = Exception("Network request failed for $resourceName with code ${response.code()}: $errorMsgBody")
                onFetchFailed(error)
                send(Resource.Error(error.message ?: "Network error", data))
            }
        } catch (throwable: Throwable) {
            Log.e("NBR_$resourceName", "Exception during fetch() or saveFetchResult(): ${throwable.message}", throwable)
            onFetchFailed(throwable)
            send(Resource.Error(throwable.message ?: "Network error during fetch/save for $resourceName", data))
        }
    } else {
        Log.d("NBR_$resourceName", "shouldFetch is FALSE. Skipping network request.")
    }
    Log.d("NBR_$resourceName", "Collecting from DB Flow...")
    flow.collect { send(it) }
    Log.d("NBR_$resourceName", "DB Flow collection finished (or flow completed).")
}