package com.ruege.mobile.data.network.api

import com.ruege.mobile.data.network.dto.response.NewsDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API сервис для получения новостей.
 */
interface NewsApiService {
    /**
     * Получает список новостей с сервера.
     * @param userId ID пользователя для получения новостей
     * @param limit максимальное количество новостей для загрузки
     * @param page номер страницы для пагинации
     */
    @GET("news")
    suspend fun getNews(): Response<List<NewsDto>>
    
    /**
     * Получает список последних новостей с сервера.
     * @param limit максимальное количество новостей для загрузки
     */
    @GET("news/latest")
    suspend fun getLatestNews(@Query("limit") limit: Int = 7): Response<List<NewsDto>>
} 