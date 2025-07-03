package com.ruege.mobile.auth

import timber.log.Timber
import com.ruege.mobile.data.network.api.AuthApiService
import com.ruege.mobile.data.network.dto.request.RefreshTokenRequestDto
import com.ruege.mobile.utils.UserDataCleaner
import com.squareup.moshi.Moshi
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
import javax.inject.Named
import javax.inject.Singleton

/**
 * Authenticator для обработки ошибок 401 и обновления Access Token.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val userDataCleaner: UserDataCleaner,
    private val authEventBus: AuthEventBus,
    private val moshi: Moshi,
    @Named("BaseUrl") private val baseUrl: String,
    private val loggingInterceptor: HttpLoggingInterceptor
) : Authenticator {

    private val TAG = "TokenAuthenticator"

    private val refreshApiService: AuthApiService by lazy { createRefreshApiService() }

    override fun authenticate(route: Route?, response: Response): Request? {
        Timber.d("Authentication required (401). URL: ${response.request.url}")
        
        val currentRefreshToken = tokenManager.getRefreshToken()

        if (currentRefreshToken == null) {
            Timber.d("No refresh token found. Cannot refresh.")
            return null
        }
        
        synchronized(this) {
            Timber.d("Entered synchronized block.")
            val newAccessTokenCheck = tokenManager.getAccessToken()
            val authHeader = response.request.header("Authorization")
            if (newAccessTokenCheck != null && authHeader != null && authHeader != "Bearer $newAccessTokenCheck") {
                Timber.d("Token seems to be refreshed already by another thread. Retrying with new token.")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessTokenCheck")
                    .build()
            }
            Timber.d("Performing token refresh request.")
            return runBlocking { 
                val (refreshed, isSessionExpired) = performTokenRefresh(currentRefreshToken)
                if (refreshed) {
                    Timber.d("Token refresh successful inside runBlocking. Retrying original request.")
                    val newToken = tokenManager.getAccessToken()
                    if (newToken != null) {
                        response.request.newBuilder()
                            .header("Authorization", "Bearer $newToken")
                            .build()
                    } else {
                        Timber.d("Token refresh succeeded but no access token was saved")
                        null
                    }
                } else {
                    Timber.d("Token refresh failed inside runBlocking.")
                    if (!isSessionExpired) {
                        tokenManager.clearTokens()
                    }
                    null
                }
            }
        } 
    }
    
    private fun createRefreshApiService(): AuthApiService {
        val refreshClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(refreshClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AuthApiService::class.java)
    }

    /**
     * Метод для выполнения запроса на обновление токена.
     * Использует отдельный OkHttpClient/Retrofit без Authenticator'а.
     * Возвращает Pair<Boolean, Boolean>, где first - успех, second - сессия истекла (401).
     */
    private suspend fun performTokenRefresh(refreshToken: String): Pair<Boolean, Boolean> {
        Timber.d("Attempting to refresh token")
        try {
            val request = RefreshTokenRequestDto(refreshToken = refreshToken)
            
            val response = refreshApiService.refreshToken(request)
            
            if (response.isSuccessful && response.body() != null) {
                val tokenDto = response.body()!!
                
                tokenManager.saveAccessToken(tokenDto.accessToken)
                tokenManager.saveRefreshToken(tokenDto.refreshToken)
                
                tokenDto.expiresIn?.let { expiresIn ->
                    tokenManager.saveTokenExpiresIn(expiresIn)
                    Timber.d("Token expires in $expiresIn seconds")
                }
                
                Timber.d("Token refresh successful. New access token: ${tokenDto.accessToken.take(10)}...")
                return Pair(true, false)
            } else {
                val errorBody = response.errorBody()?.string() ?: "No error body"
                Timber.d("Token refresh failed with code: ${response.code()}. Error: $errorBody")
                if (response.code() == 401) {
                    Timber.d("Refresh token is invalid (401). Forcing user logout.")
                    userDataCleaner.clearUserData(true)
                    authEventBus.postEvent(AuthEvent.SessionExpired)
                    return Pair(false, true)
                }
                return Pair(false, false)
            }
        } catch (e: Exception) {
            Timber.d("Exception during token refresh: ${e.message}", e)
            return Pair(false, false)
        }
    }
} 