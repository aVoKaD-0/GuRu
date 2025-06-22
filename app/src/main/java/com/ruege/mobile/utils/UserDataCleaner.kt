package com.ruege.mobile.utilss

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
            
            tokenManager.clearTokens()
            Log.d(TAG, "Токены очищены")
            
            appPreferences.clearUserPreferences()
            Log.d(TAG, "Настройки пользователя сброшены")
            
            if (clearAllData) {
                appDatabase.clearAllTables()
                Log.d(TAG, "Все таблицы базы данных очищены")
            } else {
                appDatabase.userDao().deleteAllUsers()
                Log.d(TAG, "Таблица пользователей очищена")
                
                appDatabase.syncQueueDao().clearAll()
                Log.d(TAG, "Очередь синхронизации очищена")
            }
            
            try {
                withContext(Dispatchers.Main) {
                    try {
                        com.bumptech.glide.Glide.get(context).clearMemory()
                        Log.d(TAG, "Memory кэш Glide очищен")
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка при очистке memory кэша Glide", e)
                    }
                }
                
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