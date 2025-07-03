package com.ruege.mobile.utils

import android.content.Context
import com.ruege.mobile.auth.TokenManager
import com.ruege.mobile.data.local.AppDatabase
import com.ruege.mobile.data.local.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

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
            Timber.d("Начинаем очистку данных пользователя. Полная очистка: $clearAllData")
            
            tokenManager.clearTokens()
            Timber.d("Токены очищены")
            
            appPreferences.clearUserPreferences()
            Timber.d("Настройки пользователя сброшены")
            
            if (clearAllData) {
                appDatabase.clearAllTables()
                Timber.d("Все таблицы базы данных очищены")
            } else {
                appDatabase.variantTaskOptionDao().deleteAll()
                appDatabase.userVariantTaskAnswerDao().deleteAll()

                appDatabase.variantTaskDao().clearAllTasks()
                appDatabase.variantSharedTextDao().clearAllSharedTexts()
                
                appDatabase.variantDao().deleteAll()

                appDatabase.userTaskAttemptDao().deleteAll()
                appDatabase.practiceAttemptDao().deleteAll()
                appDatabase.taskOptionDao().deleteAll()

                appDatabase.taskDao().deleteAll()
                appDatabase.taskTextDao().deleteAll()

                appDatabase.progressDao().deleteAll()
                appDatabase.practiceStatisticsDao().deleteAll()
                appDatabase.shpargalkaDao().deleteAll()
                appDatabase.downloadedTheoryDao().deleteAll()

                appDatabase.contentDao().deleteAll()
                appDatabase.categoryDao().deleteAll()
                
                appDatabase.newsDao().deleteAll()

                appDatabase.progressSyncQueueDao().deleteAll()
                appDatabase.syncQueueDao().deleteAll()
                
                appDatabase.userDao().deleteAll()
                
                Timber.d("Все пользовательские данные из таблиц очищены")
            }
            
            try {
                withContext(Dispatchers.Main) {
                    try {
                        com.bumptech.glide.Glide.get(context).clearMemory()
                        Timber.d("Memory кэш Glide очищен")
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка при очистке memory кэша Glide")
                    }
                }
                
                Thread {
                    try {
                        com.bumptech.glide.Glide.get(context).clearDiskCache()
                        Timber.d("Disk кэш Glide очищен")
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка при очистке disk кэша Glide")
                    }
                }.start()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка при очистке кэша Glide")
            }
            
            Timber.d("Очистка данных пользователя завершена")
        } catch (e: Exception) {
            Timber.e(e, "Ошибка при очистке данных пользователя")
            throw e
        }
    }
} 