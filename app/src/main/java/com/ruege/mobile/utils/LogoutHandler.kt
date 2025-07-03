package com.ruege.mobile.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.ruege.mobile.auth.GoogleAuthManager
import com.ruege.mobile.auth.TokenManager
import com.ruege.mobile.data.repository.AuthRepository
import com.ruege.mobile.ui.activity.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Обработчик логики выхода из аккаунта
 */
@Singleton
class LogoutHandler @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository,
    private val userDataCleaner: UserDataCleaner
) {
    private val TAG = "LogoutHandler"
    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Выполняет процесс выхода из аккаунта
     *
     * @param context Контекст для отображения UI
     * @param googleAuthManager Менеджер авторизации Google
     * @param categoryDataCache Кэш данных категорий для очистки
     */
    fun performLogout(
        context: Context,
        googleAuthManager: GoogleAuthManager,
        categoryDataCache: Map<String, *>
    ) {
        Timber.d("Logout initiated")
        
        val progressDialog = AlertDialog.Builder(context)
            .setTitle("Выход из аккаунта")
            .setMessage("Пожалуйста, подождите...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        val refreshToken = tokenManager.getRefreshToken()
        
        handlerScope.launch {
            try {
                if (!refreshToken.isNullOrEmpty()) {
                    try {
                        Timber.d("Отправка запроса на инвалидацию token на сервере")
                        authRepository.logout(refreshToken)
                        Timber.d("Запрос на logout успешно отправлен")
                    } catch (e: Exception) {
                        Timber.w(e, "Не удалось инвалидировать токен на сервере (возможно, он уже недействителен). Продолжаем выход...")
                    }
                } else {
                    Timber.d("Refresh token отсутствует, пропускаем запрос на сервер")
                }

                userDataCleaner.clearUserData(false)
                Timber.d("Данные пользователя очищены")
                
                if (categoryDataCache is MutableMap<*, *>) {
                    (categoryDataCache as MutableMap<*, *>).clear()
                    Timber.d("Кэш категорий очищен")
                } else {
                    Timber.w("Не удалось очистить кэш категорий - не является MutableMap")
                }
                
                withContext(Dispatchers.Main) {
                    googleAuthManager.signOut()
                }
                
                delay(800)
                
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    
                    Toast.makeText(context, "Вы успешно вышли из аккаунта", Toast.LENGTH_SHORT).show()
                    
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    context.startActivity(intent)
                    
                    if (context is android.app.Activity) {
                        context.finish()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при выходе из аккаунта")
                
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(context, "Ошибка при выходе из аккаунта", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} 