package com.ruege.mobile.utils

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
import com.ruege.mobile.ui.login.LoginActivity
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
        
        // Показываем прогресс диалог
        val progressDialog = AlertDialog.Builder(context)
            .setTitle("Выход из аккаунта")
            .setMessage("Пожалуйста, подождите...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        // Получаем refresh token перед его очисткой
        val refreshToken = tokenManager.getRefreshToken()
        
        // Запускаем корутину
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Шаг 1: Отправляем запрос на инвалидацию токена на сервере
                if (!refreshToken.isNullOrEmpty()) {
                    Log.d(TAG, "Отправка запроса на инвалидацию token на сервере")
                    authRepository.logout(refreshToken)
                    Log.d(TAG, "Запрос на logout успешно отправлен")
                } else {
                    Log.d(TAG, "Refresh token отсутствует, пропускаем запрос на сервер")
                }

                // Шаг 2: Очищаем данные пользователя (не очищаем весь контент)
                userDataCleaner.clearUserData(false)
                Log.d(TAG, "Данные пользователя очищены")
                
                // Шаг 3: Очищаем кэши в памяти
                if (categoryDataCache is MutableMap<*, *>) {
                    (categoryDataCache as MutableMap<*, *>).clear()
                    Log.d(TAG, "Кэш категорий очищен")
                } else {
                    Log.w(TAG, "Не удалось очистить кэш категорий - не является MutableMap")
                }
                
                // Шаг 4: Выходим из Google аккаунта
                withContext(Dispatchers.Main) {
                    googleAuthManager.signOut()
                }
                
                // Небольшая задержка чтобы пользователь увидел прогресс
                delay(800)
                
                // Возвращаемся в UI поток для обновления интерфейса
                withContext(Dispatchers.Main) {
                    // Закрываем диалог
                    progressDialog.dismiss()
                    
                    // Показываем уведомление об успешном выходе
                    Toast.makeText(context, "Вы успешно вышли из аккаунта", Toast.LENGTH_SHORT).show()
                    
                    // Запускаем экран входа
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    context.startActivity(intent)
                    
                    // Если контекст - это активность, закрываем её
                    if (context is android.app.Activity) {
                        context.finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при выходе из аккаунта", e)
                
                // Показываем ошибку в UI потоке
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(context, "Ошибка при выходе из аккаунта", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} 