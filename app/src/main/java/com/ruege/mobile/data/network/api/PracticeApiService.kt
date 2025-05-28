package com.ruege.mobile.data.network.api

import com.ruege.mobile.data.network.dto.request.PracticeSyncRequest
import com.ruege.mobile.data.network.dto.response.PracticeSyncResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface PracticeApiService {

    /**
     * Полная синхронизация статистики практики.
     * Отправляет все локальные данные, получает все серверные данные.
     */
    @GET("practice-statistics/sync") 
    suspend fun syncPracticeStatistics(
        @Body practiceSyncRequest: PracticeSyncRequest
    ): Response<PracticeSyncResponse>

    /**
     * Обновление (branch/merge) статистики практики.
     * Отправляет только измененные/новые локальные данные.
     * Сервер решает, как их смержить.
     */
    @POST("practice-statistics/branch") 
    suspend fun updatePracticeStatistics(
        @Body practiceSyncRequest: PracticeSyncRequest 
    ): Response<PracticeSyncResponse> 

} 