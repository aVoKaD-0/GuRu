package com.ruege.mobile.data.network.interceptor

import android.util.Log
import com.ruege.mobile.data.local.TokenManager
import com.ruege.mobile.data.network.api.AuthApiService
import com.ruege.mobile.data.network.authenticator.TokenAuthenticator
import com.ruege.mobile.data.network.dto.request.RefreshTokenRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Interceptor для добавления Authorization заголовка с Access Token.
 * Также проверяет, не истек ли срок действия токена, и проактивно обновляет его при необходимости.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiServiceProvider: Provider<AuthApiService>
) : Interceptor {

    private val TAG = "AuthInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        if (isAuthRequest(originalRequest.url.encodedPath)) {
            return chain.proceed(originalRequest)
        }

        if (tokenManager.isAccessTokenExpired(bufferSeconds = 30) && !isAuthRequest(originalRequest.url.encodedPath)) {
            Log.d(TAG, "Токен доступа скоро истечет, проактивно обновляем")
            
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken != null) {
                synchronized(this) {
                    runBlocking {
                        try {
                            val request = RefreshTokenRequestDto(refreshToken = refreshToken)
                            val authService = authApiServiceProvider.get()
                            val response = authService.refreshToken(request)
                            
                            if (response.isSuccessful && response.body() != null) {
                                val tokenDto = response.body()!!
                                tokenManager.saveAccessToken(tokenDto.accessToken)
                                tokenManager.saveRefreshToken(tokenDto.refreshToken)
                                tokenDto.expiresIn?.let { expiresIn ->
                                    tokenManager.saveTokenExpiresIn(expiresIn)
                                }
                                Log.d(TAG, "Токен успешно обновлен проактивно")
                            } else {
                                Log.e(TAG, "Не удалось проактивно обновить токен, код: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Ошибка при проактивном обновлении токена: ${e.message}")
                        }
                    }
                }
            }
        }

        val accessToken = tokenManager.getAccessToken()

        val requestBuilder = originalRequest.newBuilder()
        if (accessToken != null) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        val request = requestBuilder.build()
        return chain.proceed(request)
    }


    private fun isAuthRequest(path: String): Boolean {
        return path.endsWith("/auth/google") || path.endsWith("/auth/refresh") || path.endsWith("/auth/login")
    }
} 