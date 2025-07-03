package com.ruege.mobile.data.network.api

import com.ruege.mobile.data.network.dto.request.EmailLoginRequestDto
import com.ruege.mobile.data.network.dto.request.GoogleLoginRequestDto
import com.ruege.mobile.data.network.dto.request.LogoutRequestDto
import com.ruege.mobile.data.network.dto.request.RefreshTokenRequestDto
import com.ruege.mobile.data.network.dto.request.RegisterStartRequestDto
import com.ruege.mobile.data.network.dto.request.SetUsernameRequestDto
import com.ruege.mobile.data.network.dto.request.VerifyRequestDto
import com.ruege.mobile.data.network.dto.request.PasswordRecoveryRequestDto
import com.ruege.mobile.data.network.dto.request.TfaEnableRequestRegisterDto
import com.ruege.mobile.data.network.dto.response.AuthResponseDto
import com.ruege.mobile.data.network.dto.response.LoginResponseWrapper
import com.ruege.mobile.data.network.dto.response.RegisterStartResponseDto
import com.ruege.mobile.data.network.dto.response.TokenDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.ruege.mobile.data.network.dto.request.ResendCodeRequestDto
import com.ruege.mobile.data.network.dto.request.VerifyTfaRequestDto

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
     * Отправляет email и пароль для регистрации.
     */
    @POST("auth/register/start")
    suspend fun registerStart(@Body request: RegisterStartRequestDto): Response<RegisterStartResponseDto>

    /**
     * Отправляет email и пароль для входа.
     */
    @POST("auth/register/username")
    suspend fun registerSetUsername(@Body request: SetUsernameRequestDto): Response<Map<String, String>>

    /**
     * Отправляет код для подтверждения email.
     */
    @POST("auth/register/verify")
    suspend fun registerVerify(@Body request: VerifyRequestDto): Response<AuthResponseDto>

    /**
     * Отправляет email и пароль для входа.
     */
    @POST("auth/login")
    suspend fun loginWithEmail(@Body request: EmailLoginRequestDto): Response<LoginResponseWrapper>

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

    @POST("auth/password-recovery/request")
    suspend fun requestPasswordRecovery(@Body request: PasswordRecoveryRequestDto): Response<Map<String, String>>

    @POST("auth/register/2fa/enable")
    suspend fun enable2faOnRegistration(@Body request: TfaEnableRequestRegisterDto): Response<Map<String, String>>

    @POST("auth/register/resend-code")
    suspend fun resendConfirmationCode(@Body request: ResendCodeRequestDto): Response<Unit>

    @POST("auth/login/2fa")
    suspend fun verifyTfaLogin(@Body request: VerifyTfaRequestDto): Response<AuthResponseDto>

    @POST("auth/2fa/enable")
    suspend fun enable2fa(): Response<Unit>

    @POST("auth/2fa/disable")
    suspend fun disable2fa(): Response<Unit>

    @POST("auth/password/change")
    suspend fun changePassword(): Response<Unit>
} 