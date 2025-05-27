package com.ruege.mobile.data.network.api

import com.ruege.mobile.data.network.dto.response.EssaySummaryDto
import com.ruege.mobile.data.network.dto.response.EssayContentDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

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
} 