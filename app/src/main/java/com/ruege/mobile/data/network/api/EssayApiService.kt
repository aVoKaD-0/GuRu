package com.ruege.mobile.data.network.api

import com.ruege.mobile.data.network.dto.response.EssaySummaryDto
import com.ruege.mobile.data.network.dto.response.EssayContentDto
import com.ruege.mobile.data.network.dto.request.EssayCheckRequest
import com.ruege.mobile.data.network.dto.response.EssayCheckAccepted
import com.ruege.mobile.data.network.dto.response.EssayCheckResult
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Body

/**
 * Retrofit API сервис для получения сочинений.
 */
interface EssayApiService {
    /**
     * Получает список всех тем сочинений.
     */
    @GET("essay") 
    suspend fun getAllEssayTopics(): Response<List<EssaySummaryDto>>

    /**
     * Получает полное содержимое сочинения по ID.
     * @param contentId ID сочинения
     */
    @GET("essay/{contentId}") 
    suspend fun getEssayContent(@Path("contentId") contentId: String): Response<EssayContentDto>

    /**
     * Отправляет сочинение на проверку.
     */
    @POST("essay/check")
    suspend fun checkEssay(@Body request: EssayCheckRequest): Response<EssayCheckAccepted>

    /**
     * Получает результат проверки сочинения.
     */
    @GET("essay/check/result/{check_id}")
    suspend fun getCheckEssayResult(@Path("check_id") checkId: String): Response<EssayCheckResult>
} 