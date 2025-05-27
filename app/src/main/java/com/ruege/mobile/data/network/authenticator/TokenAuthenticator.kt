package com.ruege.mobile.data.network.authenticator

import android.util.Log
import com.ruege.mobile.data.local.TokenManager
import com.ruege.mobile.data.network.api.AuthApiService
import com.ruege.mobile.data.network.dto.request.RefreshTokenRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Provider

/**
 * Authenticator для обработки ошибок 401 и обновления Access Token.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiServiceProvider: Provider<AuthApiService>
) : Authenticator {

    private val TAG = "TokenAuthenticator"

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d(TAG, "Authentication required (401). URL: ${response.request.url}")
        
        val currentRefreshToken = tokenManager.getRefreshToken()

        if (currentRefreshToken == null) {
            Log.w(TAG, "No refresh token found. Cannot refresh.")
            return null
        }
        
        synchronized(this) {
            Log.d(TAG, "Entered synchronized block.")
            val newAccessTokenCheck = tokenManager.getAccessToken()
            val authHeader = response.request.header("Authorization")
            if (newAccessTokenCheck != null && authHeader != null && authHeader != "Bearer $newAccessTokenCheck") {
                Log.d(TAG, "Token seems to be refreshed already by another thread. Retrying with new token.")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessTokenCheck")
                    .build()
            }
            Log.d(TAG, "Performing token refresh request.")
            return runBlocking { 
                val refreshed = performTokenRefresh(currentRefreshToken)
                if (refreshed) {
                    Log.d(TAG, "Token refresh successful inside runBlocking. Retrying original request.")
                    val newToken = tokenManager.getAccessToken()
                    if (newToken != null) {
                        response.request.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                    } else {
                        Log.e(TAG, "Token refresh succeeded but no access token was saved")
                        null
                    }
                } else {
                    Log.w(TAG, "Token refresh failed inside runBlocking.")
                    tokenManager.clearTokens()
                    null
                }
            }
        } 
    }
    
    /**
     * Метод для выполнения запроса на обновление токена.
     * Использует отдельный OkHttpClient/Retrofit без Authenticator'а.
     * Возвращает true при успехе, false при ошибке.
     */
    private suspend fun performTokenRefresh(refreshToken: String): Boolean {
        Log.d(TAG, "Attempting to refresh token")
        try {
            val authApiService = authApiServiceProvider.get()
            
            val request = RefreshTokenRequestDto(refreshToken = refreshToken)
            
            val response = authApiService.refreshToken(request)
            
            if (response.isSuccessful && response.body() != null) {
                val tokenDto = response.body()!!
                
                tokenManager.saveAccessToken(tokenDto.accessToken)
                tokenManager.saveRefreshToken(tokenDto.refreshToken)
                
                tokenDto.expiresIn?.let { expiresIn ->
                    tokenManager.saveTokenExpiresIn(expiresIn)
                    Log.d(TAG, "Token expires in $expiresIn seconds")
                }
                
                Log.d(TAG, "Token refresh successful. New access token: ${tokenDto.accessToken.take(10)}...")
                return true
            } else {
                val errorBody = response.errorBody()?.string() ?: "No error body"
                Log.e(TAG, "Token refresh failed with code: ${response.code()}. Error: $errorBody")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token refresh: ${e.message}", e)
            return false
        }
    }
} 