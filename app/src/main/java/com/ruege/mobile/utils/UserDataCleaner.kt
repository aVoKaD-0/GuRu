package com.ruege.mobile.utils

import android.content.Context
import android.util.Log
import com.ruege.mobile.data.local.TokenManager
import com.ruege.mobile.data.local.AppDatabase
import com.ruege.mobile.data.local.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Утилита для очистки данных пользователя при выходе из аккаунта
 */
@Singleton
class UserDataCleaner @Inject constructor(
    private val tokenManager: TokenManager,
    private val appDatabase: AppDatabase,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context
) {
    private val TAG = "UserDataCleaner"

    /**
     * Очищает все данные пользователя при выходе из аккаунта
     * @param clearAllData если true, очищает все данные, включая прогресс. Если false, сохраняет некоторые данные для локального использования.
     */
    suspend fun clearUserData(clearAllData: Boolean) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Начинаем очистку данных пользователя. Полная очистка: $clearAllData")
            
            // Очищаем токены (всегда)
            tokenManager.clearTokens()
            Log.d(TAG, "Токены очищены")
            
            // Сбрасываем настройки пользователя
            appPreferences.clearUserPreferences()
            Log.d(TAG, "Настройки пользователя сброшены")
            
            if (clearAllData) {
                // Полная очистка - сбрасываем все данные в базе
                // Это радикальный подход, очищающий вообще все данные
                // Используйте осторожно, особенно если некоторые данные должны сохраняться между сессиями
                appDatabase.clearAllTables()
                Log.d(TAG, "Все таблицы базы данных очищены")
            } else {
                // Выборочная очистка - удаляем только данные пользователя
                // Но сохраняем кэшированный контент и прогресс для возможности работы оффлайн
                
                // Очищаем таблицу пользователей
                appDatabase.userDao().deleteAllUsers()
                Log.d(TAG, "Таблица пользователей очищена")
                
                // Очищаем связанные с синхронизацией таблицы
                appDatabase.syncQueueDao().clearAll()
                Log.d(TAG, "Очередь синхронизации очищена")
            }
            
            // Очищаем кэш Glide
            try {
                // clearMemory должен выполняться на главном потоке
                withContext(Dispatchers.Main) {
                    try {
                        com.bumptech.glide.Glide.get(context).clearMemory()
                        Log.d(TAG, "Memory кэш Glide очищен")
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при очистке memory кэша Glide", e)
                    }
                }
                
                // Запускаем очистку дискового кэша в отдельном потоке
                // (это уже правильно, так как clearDiskCache должен выполняться НЕ на главном потоке)
                Thread {
                    try {
                        com.bumptech.glide.Glide.get(context).clearDiskCache()
                        Log.d(TAG, "Disk кэш Glide очищен")
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при очистке disk кэша Glide", e)
                    }
                }.start()
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при очистке кэша Glide", e)
            }
            
            Log.d(TAG, "Очистка данных пользователя завершена")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при очистке данных пользователя", e)
            throw e
        }
    }
} 