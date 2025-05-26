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

    private val mutex = Mutex() // Для синхронизации обновления токена
    private val isRefreshing = AtomicBoolean(false)

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d(TAG, "Получен 401, попытка обновления токена")
        
        // Пропускаем, если это уже попытка обновления токена
        val requestUrl = response.request.url.toString()
        if (requestUrl.contains("/auth/refresh")) {
            Log.d(TAG, "Пропускаем обновление токена для запроса /auth/refresh")
            return null
        }
        
        // Избегаем множественных запросов на обновление
        if (isRefreshing.getAndSet(true)) {
            Log.d(TAG, "Уже идет процесс обновления токена, ожидаем...")
            // Другая операция уже обновляет токен, просто подождем
            Thread.sleep(1000) // Даем время другой операции обновить токен
            isRefreshing.set(false)
            
            // Проверяем, есть ли теперь новый токен
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
            // Получаем refresh token
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken == null) {
                Log.e(TAG, "Refresh token отсутствует, нельзя обновить токен")
                isRefreshing.set(false)
                return null
            }
            
            // Выполняем запрос на обновление токена
            val result = runBlocking {
                mutex.withLock {
                    authRepository.refreshToken(refreshToken)
                }
            }
            
            // Обрабатываем результат
            if (result is com.ruege.mobile.utils.Resource.Success && result.data != null) {
                // Сохраняем новые токены
                tokenManager.saveAccessToken(result.data.accessToken)
                tokenManager.saveRefreshToken(result.data.refreshToken)
                Log.d(TAG, "Токен успешно обновлен, повторяем запрос")
                
                // Создаем новый запрос с новым токеном
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${result.data.accessToken}")
                    .build()
            } else {
                // Обновление не удалось, очищаем токены
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