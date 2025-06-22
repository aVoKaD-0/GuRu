package com.ruege.mobile.data.network.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import javax.inject.Singleton

/**
 * API сервис для работы со шпаргалками
 */
@Singleton
interface ShpargalkiApiService {
    
    /**
     * Получение списка шпаргалок (название, описание, время публикации)
     */
    @GET("shpargalki/groups")
    suspend fun getShpargalkaGroups(): Response<List<Map<String, Any>>>
    
    /**
     * Получение PDF файла шпаргалки по ID
     */
    @GET("shpargalki/{pdf_id}")
    suspend fun getShpargalkaPdf(@Path("pdf_id") pdfId: Int): Response<ResponseBody>
}