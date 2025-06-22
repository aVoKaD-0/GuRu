package com.ruege.mobile.data.network.api

import com.ruege.mobile.data.network.dto.request.PracticeStatisticsBranchRequest
import com.ruege.mobile.data.network.dto.response.PracticeStatisticsBranchResponse
import com.ruege.mobile.data.network.dto.response.PracticeStatisticsGetResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PracticeApiService {

    /**
     * Полная синхронизация статистики практики.
     * Отправляет все локальные данные, получает все серверные данные.
     */
    @GET("practiceStatistics/practice-statistics/sync") 
    suspend fun syncPracticeStatistics(
        @Query("timestamp") timestamp: Long?
    ): Response<PracticeStatisticsGetResponse>

    /**
     * Обновление (branch/merge) статистики практики.
     * Отправляет только измененные/новые локальные данные.
     * Сервер решает, как их смержить.
     */
    @POST("practiceStatistics/practice-statistics/branch")
    suspend fun updatePracticeStatistics(
        @Body practiceSyncRequest: PracticeStatisticsBranchRequest
    ): Response<PracticeStatisticsBranchResponse>

} 