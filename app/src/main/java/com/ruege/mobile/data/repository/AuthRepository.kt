package com.ruege.mobile.data.repository

import android.util.Log
import com.ruege.mobile.data.local.dao.UserDao
import com.ruege.mobile.data.local.entity.UserEntity
import com.ruege.mobile.data.network.api.AuthApiService
import com.ruege.mobile.data.network.dto.request.GoogleLoginRequestDto
import com.ruege.mobile.data.network.dto.request.LogoutRequestDto
import com.ruege.mobile.data.network.dto.request.RefreshTokenRequestDto
import com.ruege.mobile.data.network.dto.response.AuthResponseDto
import com.ruege.mobile.data.network.dto.response.TokenDto
import com.ruege.mobile.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий для аутентификации.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val userDao: UserDao
) {

    /**
     * Выполняет вход через Google, отправляя токен на бэкенд.
     * @param googleIdToken Google ID токен.
     * @return Resource<AuthResponseDto> с результатом, включающим токены и данные пользователя.
     */
    suspend fun loginWithGoogle(googleIdToken: String): Resource<AuthResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                // Дополнительное логирование для проверки формата токена
                val segments = googleIdToken.split(".")
                Log.d(TAG, "Google ID Token debug: Количество сегментов: ${segments.size}")
                Log.d(TAG, "Google ID Token debug: Начало: ${googleIdToken.take(20)}...")
                
                Log.d(TAG, "Начало авторизации через Google. Длина ID токена: ${googleIdToken.length}")
                val requestDto = GoogleLoginRequestDto(googleIdToken = googleIdToken)
                Log.d(TAG, "Отправка запроса на сервер...")
                val response = authApiService.loginWithGoogle(requestDto)
                Log.d(TAG, "Получен ответ от сервера. Код: ${response.code()}")

                if (response.isSuccessful) {
                    response.body()?.let { authResponseDto ->
                        Log.d(TAG, "Google login успешен. Access token: ${authResponseDto.accessToken.take(10)}..., User: ${authResponseDto.user.username}")
                        
                        // Сохраняем пользователя в БД
                        try {
                            val userDto = authResponseDto.user
                            val userEntity = UserEntity(
                                // userId устанавливается из DTO, т.к. он приходит с бэка
                                // createdAt и lastLogin требуют преобразования из String в Long (timestamp)
                            )
                            userEntity.setUserId(userDto.userId.toLong())
                            userEntity.setUsername(userDto.username ?: "")
                            userEntity.setEmail(userDto.email ?: "")
                            userEntity.setAvatarUrl(userDto.avatarUrl ?: "")
                            userEntity.setCreatedAt(parseDateStringToTimestamp(userDto.createdAt, System.currentTimeMillis())) // Fallback to current time if parsing fails for createdAt
                            userEntity.setLastLogin(parseDateStringToTimestamp(userDto.lastLogin, System.currentTimeMillis()))    // Fallback to current time for lastLogin
                            // Устанавливаем googleId в пустую строку, так как это поле не может быть null
                            userEntity.setGoogleId(userDto.googleId ?: "")

                            userDao.insert(userEntity)
                            Log.d(TAG, "Пользователь ${userEntity.getUsername()} (ID: ${userEntity.getUserId()}) сохранен/обновлен в БД.")

                        } catch (e: Exception) {
                            Log.e(TAG, "Ошибка при сохранении пользователя в БД: ${e.message}", e)
                            // Решаем, должна ли эта ошибка приводить к Resource.Error для всего логина
                            // Пока что логируем и продолжаем, возвращая токены и данные пользователя
                        }
                        
                        Resource.Success(authResponseDto)
                    } ?: run {
                        Log.e(TAG, "Google login: тело ответа пусто при успешном коде ${response.code()}")
                        Resource.Error("Ответ сервера пуст", null)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Нет деталей ошибки"
                    Log.e(TAG, "Google login неудачен. Код: ${response.code()}, Ошибка: $errorBody")
                    Resource.Error("Ошибка входа (${response.code()}): $errorBody", null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при Google login: ${e.javaClass.simpleName} - ${e.message}", e)
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}", null)
            }
        }
    }

    private fun parseDateStringToTimestamp(dateString: String?, defaultTimestamp: Long): Long {
        if (dateString.isNullOrBlank()) {
            Log.w(TAG, "Дата для парсинга пуста, используется значение по умолчанию: $defaultTimestamp")
            return defaultTimestamp
        }
        return try {
            // Пример формата от сервера: "2025-04-29 14:31:19.043744+00"
            // Заменяем пробел на 'T' для соответствия ISO_OFFSET_DATE_TIME
            val cleanedDateString = dateString.replace(" ", "T")
            OffsetDateTime.parse(cleanedDateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        } catch (e: DateTimeParseException) {
            Log.e(TAG, "Ошибка парсинга строки даты '$dateString': ${e.message}", e)
            defaultTimestamp // Возвращаем значение по умолчанию при ошибке
        } catch (e: Exception) {
            Log.e(TAG, "Неожиданная ошибка при парсинге даты '$dateString': ${e.message}", e)
            defaultTimestamp
        }
    }
    
    /**
     * Обновляет токен доступа, используя refresh токен.
     * @param refreshToken Refresh токен.
     * @return Resource<TokenDto> с результатом.
     */
    suspend fun refreshToken(refreshToken: String): Resource<TokenDto> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Начало обновления токена. Длина Refresh токена: ${refreshToken.length}")
                val request = RefreshTokenRequestDto(refreshToken = refreshToken)
                Log.d(TAG, "Отправка запроса на обновление токена...")
                val response = authApiService.refreshToken(request)
                Log.d(TAG, "Получен ответ от сервера. Код: ${response.code()}")

                if (response.isSuccessful) {
                    response.body()?.let { tokenDto ->
                        Log.d(TAG, "Токен успешно обновлен. Новый Access token: ${tokenDto.accessToken.take(10)}..., длина: ${tokenDto.accessToken.length}")
                        Resource.Success(tokenDto)
                    } ?: run {
                        Log.e(TAG, "Обновление токена: тело ответа пусто при успешном коде ${response.code()}")
                        Resource.Error("Ответ сервера пуст", null)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Нет деталей ошибки"
                    Log.e(TAG, "Обновление токена неудачно. Код: ${response.code()}, Ошибка: $errorBody")
                    Resource.Error("Ошибка обновления токена (${response.code()}): $errorBody", null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при обновлении токена: ${e.javaClass.simpleName} - ${e.message}", e)
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}", null)
            }
        }
    }

    /**
     * Выполняет выход из аккаунта, инвалидируя refresh токен на сервере.
     * @param refreshToken Refresh токен для инвалидации.
     * @return Resource<Boolean> с результатом операции.
     */
    suspend fun logout(refreshToken: String): Resource<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Начало выхода из аккаунта. Invalidating refresh token")
                val request = LogoutRequestDto(refreshToken = refreshToken)
                val response = authApiService.logout(request)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Выход из аккаунта выполнен успешно. Токен инвалидирован на сервере")
                    Resource.Success(true)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Нет деталей ошибки"
                    Log.e(TAG, "Выход из аккаунта неудачен. Код: ${response.code()}, Ошибка: $errorBody")
                    // Будем возвращать успех даже при ошибке на сервере, так как локально мы все равно должны очистить данные
                    // Это поведение можно изменить, если требуется строгая синхронизация с сервером
                    Resource.Success(true) 
                }
            } catch (e: Exception) {
                Log.e(TAG, "Исключение при выходе из аккаунта: ${e.javaClass.simpleName} - ${e.message}", e)
                // Также возвращаем успех при ошибках сети, чтобы пользователь мог выйти даже без интернета
                Resource.Success(true)
            }
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
} 