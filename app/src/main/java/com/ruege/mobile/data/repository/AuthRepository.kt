package com.ruege.mobile.data.repository

import timber.log.Timber
import com.ruege.mobile.data.local.dao.UserDao
import com.ruege.mobile.data.local.entity.UserEntity
import com.ruege.mobile.data.network.api.AuthApiService
import com.ruege.mobile.data.network.dto.request.*
import com.ruege.mobile.data.network.dto.response.AuthResponseDto
import com.ruege.mobile.data.network.dto.response.RegisterStartResponseDto
import com.ruege.mobile.data.network.dto.response.TokenDto
import com.ruege.mobile.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
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
                val requestDto = GoogleLoginRequestDto(googleIdToken = googleIdToken)
                val response = authApiService.loginWithGoogle(requestDto)

                if (response.isSuccessful) {
                    response.body()?.let { authResponseDto ->
                        val userDto = authResponseDto.user
                        val userEntity = UserEntity().apply {
                            this.userId = userDto.userId.toLong()
                            this.username = userDto.username ?: ""
                            this.email = userDto.email ?: ""
                            this.avatarUrl = userDto.avatarUrl ?: ""
                            this.createdAt = parseDateStringToTimestamp(userDto.createdAt, System.currentTimeMillis())
                            this.lastLogin = userDto.lastLogin?.let { parseDateStringToTimestamp(it, 0L) } ?: 0L
                            this.isIs2faEnabled = userDto.is2faEnabled ?: false
                        }
                        userDao.insert(userEntity)
                        Resource.Success(authResponseDto)
                    } ?: Resource.Error("Ответ сервера пуст", null)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Нет деталей ошибки"
                    Resource.Error("Ошибка входа (${response.code()}): $errorBody", null)
                }
            } catch (e: Exception) {
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}", null)
            }
        }
    }

    suspend fun registerStart(email: String, password: String, recaptchaToken: String): Resource<RegisterStartResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val requestDto = RegisterStartRequestDto(email = email, password = password, recaptcha_token = recaptchaToken)
                val response = authApiService.registerStart(requestDto)
                if (response.isSuccessful) {
                    response.body()?.let { Resource.Success(it) } ?: Resource.Error("Ответ сервера пуст")
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Нет деталей ошибки"
                    Resource.Error("Ошибка (${response.code()}): $errorBody")
                }
            } catch (e: Exception) {
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }

    suspend fun setUsername(sessionToken: String, username: String): Resource<Map<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val requestDto = SetUsernameRequestDto(session_token = sessionToken, username = username)
                Timber.d("session: ${sessionToken}, username: ${username}")
                val response = authApiService.registerSetUsername(requestDto)
                if (response.isSuccessful) {
                    response.body()?.let { Resource.Success(it) } ?: Resource.Success(emptyMap())
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Нет деталей ошибки"
                    Resource.Error("Ошибка (${response.code()}): $errorBody")
                }
            } catch (e: Exception) {
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }

    suspend fun verifyCode(sessionToken: String, code: String): Resource<AuthResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val requestDto = VerifyRequestDto(session_token = sessionToken, code = code)
                val response = authApiService.registerVerify(requestDto)
                if (response.isSuccessful) {
                    response.body()?.let { authResponseDto ->
                        Timber.d("Verification successful. Response: $authResponseDto")
                        val userDto = authResponseDto.user
                        val userEntity = UserEntity().apply {
                            this.userId = userDto.userId.toLong()
                            this.username = userDto.username ?: ""
                            this.email = userDto.email ?: ""
                            this.avatarUrl = userDto.avatarUrl ?: ""
                            this.createdAt = parseDateStringToTimestamp(userDto.createdAt, System.currentTimeMillis())
                            this.lastLogin = userDto.lastLogin?.let { parseDateStringToTimestamp(it, 0L) } ?: 0L
                            this.isIs2faEnabled = userDto.is2faEnabled ?: false
                        }
                        userDao.insert(userEntity)
                        Resource.Success(authResponseDto)
                    } ?: Resource.Error("Ответ сервера пуст")
                } else {
                    val errorBody = if (response.code() == 400) {
                        "неверный код"
                    } else {
                        response.errorBody()?.string() ?: "Неизвестная ошибка"
                    }
                    Resource.Error(errorBody)
                }
            } catch (e: Exception) {
                Timber.d("Exception during code verification: ${e.message}", e)
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }

    suspend fun loginWithEmail(email: String, password: String): Resource<LoginResult> {
        return withContext(Dispatchers.IO) {
            try {
                val requestDto = EmailLoginRequestDto(email = email, password = password, recaptchaToken = null)
                val response = authApiService.loginWithEmail(requestDto)

                if (response.isSuccessful) {
                    val wrapper = response.body()
                    Timber.d("AuthRepository", "Server Response Body: $wrapper")

                    val authResponse = wrapper?.toAuthResponseDto()
                    val tfaResponse = wrapper?.toLogin2faResponseDto()

                    when {
                        authResponse != null -> {
                            val userDto = authResponse.user
                            val userEntity = UserEntity().apply {
                                this.userId = userDto.userId.toLong()
                                this.username = userDto.username ?: ""
                                this.email = userDto.email ?: ""
                                this.avatarUrl = userDto.avatarUrl ?: ""
                                this.createdAt = parseDateStringToTimestamp(userDto.createdAt, System.currentTimeMillis())
                                this.lastLogin = userDto.lastLogin?.let { parseDateStringToTimestamp(it, 0L) } ?: 0L
                                this.isIs2faEnabled = userDto.is2faEnabled ?: false
                            }
                            userDao.insert(userEntity)
                            Resource.Success(LoginResult.Success(authResponse))
                        }
                        tfaResponse != null -> {
                            Resource.Success(LoginResult.TwoFactorRequired(tfaResponse))
                        }
                        else -> {
                            val errorMsg = "Некорректный ответ от сервера. Получено: $wrapper"
                            Timber.e("AuthRepository", errorMsg)
                            Resource.Error(errorMsg)
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Нет деталей ошибки"
                    Resource.Error("Ошибка входа (${response.code()}): $errorBody")
                }
            } catch (e: Exception) {
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }
    
    private fun parseDateStringToTimestamp(dateString: String?, defaultTimestamp: Long): Long {
        if (dateString.isNullOrBlank()) return defaultTimestamp
        return try {
            OffsetDateTime.parse(dateString.replace(" ", "T"), DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant().toEpochMilli()
        } catch (e: Exception) {
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
                val request = RefreshTokenRequestDto(refreshToken = refreshToken)
                val response = authApiService.refreshToken(request)
                if (response.isSuccessful) {
                    response.body()?.let { Resource.Success(it) } ?: Resource.Error("Ответ сервера пуст", null)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Нет деталей ошибки"
                    Resource.Error("Ошибка обновления токена (${response.code()}): $errorBody", null)
                }
            } catch (e: Exception) {
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
                val request = LogoutRequestDto(refreshToken = refreshToken)
                authApiService.logout(request)
                Resource.Success(true)
            } catch (e: Exception) {
                Resource.Success(true)
            }
        }
    }

    suspend fun requestPasswordRecovery(email: String, recaptchaToken: String): Resource<Map<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val requestDto = PasswordRecoveryRequestDto(email = email, recaptchaToken = recaptchaToken)
                val response = authApiService.requestPasswordRecovery(requestDto)
                if (response.isSuccessful) {
                    response.body()?.let { Resource.Success(it) } ?: Resource.Success(emptyMap())
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Нет деталей ошибки"
                    Resource.Error("Ошибка (${response.code()}): $errorBody")
                }
            } catch (e: Exception) {
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }

    suspend fun enable2faOnRegistration(sessionToken: String, userId: Int): Resource<Map<String, String>> {
        return withContext(Dispatchers.IO) {
            try {
                val requestDto = TfaEnableRequestRegisterDto(session_token = sessionToken, user_id = userId)
                val response = authApiService.enable2faOnRegistration(requestDto)
                if (response.isSuccessful) {
                    response.body()?.let { Resource.Success(it) } ?: Resource.Success(emptyMap())
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Нет деталей ошибки"
                    Resource.Error("Ошибка (${response.code()}): $errorBody")
                }
            } catch (e: Exception) {
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }

    suspend fun verifyTfaLogin(loginSessionToken: String, tfaCode: String): Resource<AuthResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val requestDto = VerifyTfaRequestDto(loginSessionToken = loginSessionToken, tfaCode = tfaCode)
                val response = authApiService.verifyTfaLogin(requestDto)
                if (response.isSuccessful) {
                    response.body()?.let { authResponse ->
                        val userDto = authResponse.user
                        val userEntity = UserEntity().apply {
                            this.userId = userDto.userId.toLong()
                            this.username = userDto.username ?: ""
                            this.email = userDto.email ?: ""
                            this.avatarUrl = userDto.avatarUrl ?: ""
                            this.createdAt = parseDateStringToTimestamp(userDto.createdAt, System.currentTimeMillis())
                            this.lastLogin = userDto.lastLogin?.let { parseDateStringToTimestamp(it, 0L) } ?: 0L
                            this.isIs2faEnabled = userDto.is2faEnabled ?: false
                        }
                        userDao.insert(userEntity)
                        Resource.Success(authResponse)
                    } ?: Resource.Error("Ответ сервера пуст")
                } else {
                    val errorBody = if (response.code() == 400) {
                        "неверный код"
                    } else {
                        response.errorBody()?.string() ?: "Неизвестная ошибка"
                    }
                    Resource.Error(errorBody)
                }
            } catch (e: Exception) {
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }

    suspend fun resendConfirmationCode(sessionToken: String): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val requestDto = ResendCodeRequestDto(sessionToken = sessionToken)
                val response = authApiService.resendConfirmationCode(requestDto)
                if (response.isSuccessful) {
                    Resource.Success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Невозможно повторно отправить код"
                    Resource.Error("Ошибка (${response.code()}): $errorBody")
                }
            } catch (e: Exception) {
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }

    suspend fun enable2fa(): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApiService.enable2fa()
                if (response.isSuccessful) {
                    Timber.d("2FA enabled successfully.")
                    Resource.Success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Timber.d("Failed to enable 2FA: ${response.code()} - $errorBody")
                    Resource.Error("Ошибка (${response.code()}): $errorBody")
                }
            } catch (e: Exception) {
                Timber.d("Network or server error when enabling 2FA: ${e.message}", e)
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }

    suspend fun disable2fa(): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApiService.disable2fa()
                if (response.isSuccessful) {
                    Timber.d("2FA disabled successfully.")
                    Resource.Success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Timber.d("Failed to disable 2FA: ${response.code()} - $errorBody")
                    Resource.Error("Ошибка (${response.code()}): $errorBody")
                }
            } catch (e: Exception) {
                Timber.d("Network or server error when disabling 2FA: ${e.message}", e)
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }

    suspend fun changePassword(): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApiService.changePassword()
                if (response.isSuccessful) {
                    Timber.d("Password change requested successfully.")
                    Resource.Success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Timber.d("Failed to request password change: ${response.code()} - $errorBody")
                    Resource.Error("Ошибка (${response.code()}): $errorBody")
                }
            } catch (e: Exception) {
                Timber.d("Network or server error when requesting password change: ${e.message}", e)
                Resource.Error("Ошибка сети или сервера: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
} 