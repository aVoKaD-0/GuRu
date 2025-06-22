package com.ruege.mobile.utilss

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.ruege.mobile.auth.GoogleAuthManager
import com.ruege.mobile.data.local.TokenManager
import com.ruege.mobile.data.repository.AuthRepository
import com.ruege.mobile.ui.activity.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Map
import javax.inject.Inject
import javax.inject.Singleton

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

    /**
     * Выполняет процесс выхода из аккаунта
     *
     * @param context Контекст для отображения UI
     * @param lifecycleOwner Владелец жизненного цикла для запуска корутин
     * @param googleAuthManager Менеджер авторизации Google
     * @param categoryDataCache Кэш данных категорий для очистки
     */
    fun performLogout(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        googleAuthManager: GoogleAuthManager,
        categoryDataCache: Map<String, *>
    ) {
        Log.d(TAG, "Logout initiated")
        
        val progressDialog = AlertDialog.Builder(context)
            .setTitle("Выход из аккаунта")
            .setMessage("Пожалуйста, подождите...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        val refreshToken = tokenManager.getRefreshToken()
        
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!refreshToken.isNullOrEmpty()) {
                    try {
                        Log.d(TAG, "Отправка запроса на инвалидацию token на сервере")
                        authRepository.logout(refreshToken)
                        Log.d(TAG, "Запрос на logout успешно отправлен")
                    } catch (e: Exception) {
                        Log.w(TAG, "Не удалось инвалидировать токен на сервере (возможно, он уже недействителен). Продолжаем выход...", e)
                    }
                } else {
                    Log.d(TAG, "Refresh token отсутствует, пропускаем запрос на сервер")
                }

                userDataCleaner.clearUserData(false)
                Log.d(TAG, "Данные пользователя очищены")
                
                if (categoryDataCache is MutableMap<*, *>) {
                    (categoryDataCache as MutableMap<*, *>).clear()
                    Log.d(TAG, "Кэш категорий очищен")
                } else {
                    Log.w(TAG, "Не удалось очистить кэш категорий - не является MutableMap")
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
                Log.e(TAG, "Ошибка при выходе из аккаунта", e)
                
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(context, "Ошибка при выходе из аккаунта", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} 