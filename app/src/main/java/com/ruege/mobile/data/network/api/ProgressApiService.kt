package com.ruege.mobile.data.network.api

import com.ruege.mobile.data.network.dto.ProgressSyncResponse
import com.ruege.mobile.data.network.dto.ProgressUpdateRequest
import com.ruege.mobile.data.network.dto.response.ProgressSyncItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * API для работы с прогрессом на сервере
 */
interface ProgressApiService {
    /**
     * Отправляет запрос на обновление прогресса
     * @param request запрос на обновление прогресса
     * @return ответ от сервера
     */
    @POST("progress")
    suspend fun updateProgress(@Body request: ProgressUpdateRequest): Response<ProgressSyncResponse>
    
    /**
     * Отправляет пакет запросов на обновление прогресса
     * @param updates список запросов на обновление прогресса
     * @return список ответов от сервера
     */
    @POST("progress/batch")
    suspend fun updateProgressBatch(@Body updates: List<ProgressUpdateRequest>): Response<List<ProgressSyncResponse>>
    
    /**
     * Запрашивает синхронизацию прогресса с сервера
     * @param timestamp временная метка последней синхронизации
     * @return список актуального прогресса
     */
    @GET("progress/sync")
    suspend fun syncProgress(@Query("timestamp") timestamp: Long?): Response<List<ProgressSyncItemDto>>
    
    /**
     * Получает прогресс для определенного контента
     * @param contentId ID контента
     * @return прогресс для указанного контента
     */
    @GET("progress/{contentId}")
    suspend fun getProgressByContentId(contentId: String): Response<com.ruege.mobile.data.local.entity.ProgressEntity>
} 