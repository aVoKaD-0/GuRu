package com.ruege.mobile

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ruege.mobile.data.local.DBResetHelper
import com.ruege.mobile.data.repository.ProgressRepository
import com.ruege.mobile.data.repository.ProgressSyncRepository
import com.ruege.mobile.worker.ProgressSyncWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class MobileApplication : Application(), Configuration.Provider {
    companion object {
        private const val TAG = "MobileApplication"
        private const val DB_VERSION = 12 // Соответствует версии базы данных в AppDatabase
        private const val FORCE_DB_RESET = false // Замените на true, если нужно принудительно сбрасывать базу данных при запуске
    }
    
    // Инжектируем сюда готовую конфигурацию из WorkManagerModule, 
    // но используем другое имя, чтобы избежать конфликта с методом getWorkManagerConfiguration()
    @Inject
    lateinit var workConfig: Configuration
    
    /**
     * Репозиторий для синхронизации прогресса, доступен из внешних классов для принудительной синхронизации
     */
    @Inject
    lateinit var progressSyncRepository: ProgressSyncRepository
    
    /**
     * Репозиторий прогресса
     */
    @Inject
    lateinit var progressRepository: ProgressRepository
    
    override fun getWorkManagerConfiguration(): Configuration {
        return workConfig
    }

    override fun onCreate() {
        super.onCreate()
        
        // Активируем Timber для логирования
        Timber.plant(Timber.DebugTree())
        
        // Инициализируем репозиторий для синхронизации прогресса
        progressSyncRepository.initialize()
        Log.d(TAG, "ProgressSyncRepository.initialize() вызван в MobileApplication")
        
        // Инициализируем репозиторий прогресса
        progressRepository.initialize()
        Log.d(TAG, "ProgressRepository.initialize() вызван в MobileApplication")

        // Проверяем необходимость сброса БД на основе версии БД        
        val preferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentDbVersion = preferences.getInt("db_version", 0)
        
        if (currentDbVersion < DB_VERSION || BuildConfig.DEBUG && FORCE_DB_RESET) {
            DBResetHelper.resetDatabase(this)
            preferences.edit().putInt("db_version", DB_VERSION).apply()
        }
        
        // Отключаем темную тему
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        
        Log.d(TAG, "MobileApplication onCreate complete")
    }
} 