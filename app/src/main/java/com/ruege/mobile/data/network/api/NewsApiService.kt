package com.ruege.mobile.data.network.api

import com.ruege.mobile.data.network.dto.response.NewsDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API сервис для получения новостей.
 */
interface NewsApiService {
    @GET("news")
    suspend fun getNews(): Response<List<NewsDto>>
    
    @GET("news/latest")
    suspend fun getLatestNews(@Query("limit") limit: Int = 7): Response<List<NewsDto>>
} 