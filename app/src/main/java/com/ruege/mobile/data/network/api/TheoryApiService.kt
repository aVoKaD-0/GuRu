package com.ruege.mobile.data.network.api

import com.ruege.mobile.data.network.dto.response.TheorySummaryDto
import com.ruege.mobile.data.network.dto.response.TheoryContentDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit API сервис для получения теории.
 */
interface TheoryApiService {
    /**
     * Получает список всех тем теории.
     */
    @GET("theory") 
    suspend fun getAllTheory(): Response<List<TheorySummaryDto>>
    
    /**
     * Получает полное содержимое теории по ID.
     * @param contentId ID теории
     */
    @GET("theory/content/{contentId}")
    suspend fun getTheoryContent(@Path("contentId") contentId: String): Response<TheoryContentDto>
} 