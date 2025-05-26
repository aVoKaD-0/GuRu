package com.ruege.mobile.data.network.api

import com.ruege.mobile.data.network.dto.VariantDetailDto
import com.ruege.mobile.data.network.dto.VariantListItemDto
import com.ruege.mobile.data.network.dto.UserAnswerPayloadDto
import com.ruege.mobile.data.network.dto.UserAnswerResponseItemDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface VariantApiService {

    @GET("variants/")
    suspend fun getVariants(): Response<List<VariantListItemDto>>

    @GET("variants/{variant_id}")
    suspend fun getVariantById(@Path("variant_id") variantId: Int): Response<VariantDetailDto>

    @POST("variants/{variant_id}/answers")
    suspend fun submitAnswers(
        @Path("variant_id") variantId: Int,
        @Body answers: List<UserAnswerPayloadDto>
    ): Response<List<UserAnswerResponseItemDto>>

    // TODO: Когда понадобится, добавить метод для получения деталей варианта
    // Например, для отображения одного конкретного варианта с заданиями
    // @GET("variants/{variant_id}")
    // suspend fun getVariantDetails(@Path("variant_id") variantId: Int): Response<VariantDetailDto> // Предполагаемый DTO
} 