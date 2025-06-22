package com.ruege.mobile.data.network.api

import com.ruege.mobile.data.network.dto.request.GoogleLoginRequestDto
import com.ruege.mobile.data.network.dto.request.LogoutRequestDto
import com.ruege.mobile.data.network.dto.request.RefreshTokenRequestDto
import com.ruege.mobile.data.network.dto.response.AuthResponseDto
import com.ruege.mobile.data.network.dto.response.TokenDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API service для аутентификации.
 */
interface AuthApiService {

    /**
     * Отправляет Google ID токен для входа или регистрации.
     */
    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequestDto): Response<AuthResponseDto>

    /**
     * Обновляет токен доступа с помощью refresh токена.
     */
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequestDto): Response<TokenDto>
    
    /**
     * Выполняет выход из аккаунта и инвалидирует refresh токен на сервере.
     */
    @POST("auth/logout-token")
    suspend fun logout(@Body request: LogoutRequestDto): Response<Map<String, String>>
} 