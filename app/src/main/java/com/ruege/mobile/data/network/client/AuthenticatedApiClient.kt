package com.ruege.mobile.data.network.client

import android.util.Log
import com.ruege.mobile.data.local.TokenManager
import com.ruege.mobile.data.network.dto.request.RefreshTokenRequestDto
import com.ruege.mobile.data.network.api.AuthApiService
import com.ruege.mobile.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Аутентификатор для автоматического обновления токена при получении 401 Unauthorized.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository
) : Authenticator {

    private val mutex = Mutex() 
    private val isRefreshing = AtomicBoolean(false)

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d(TAG, "Получен 401, попытка обновления токена")
    
        val requestUrl = response.request.url.toString()
        if (requestUrl.contains("/auth/refresh")) {
            Log.d(TAG, "Пропускаем обновление токена для запроса /auth/refresh")
            return null
        }
        
        if (isRefreshing.getAndSet(true)) {
            Thread.sleep(1000)
            isRefreshing.set(false)
            
            val newToken = tokenManager.getAccessToken()
            if (newToken != null) {
                Log.d(TAG, "Получен новый токен, повторяем запрос")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }
            return null
        }
        
        try {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken == null) {
                Log.e(TAG, "Refresh token отсутствует, нельзя обновить токен")
                isRefreshing.set(false)
                return null
            }
            
            val result = runBlocking {
                mutex.withLock {
                    authRepository.refreshToken(refreshToken)
                }
            }
            
            if (result is com.ruege.mobile.utils.Resource.Success && result.data != null) {
                tokenManager.saveAccessToken(result.data.accessToken)
                tokenManager.saveRefreshToken(result.data.refreshToken)
                Log.d(TAG, "Токен успешно обновлен, повторяем запрос")
                
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${result.data.accessToken}")
                    .build()
            } else {
                Log.e(TAG, "Не удалось обновить токен: ${(result as? com.ruege.mobile.utils.Resource.Error)?.message}")
                tokenManager.clearTokens()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при обновлении токена: ${e.message}", e)
        } finally {
            isRefreshing.set(false)
        }
        
        return null
    }
    
    companion object {
        private const val TAG = "TokenAuthenticator"
    }
} 