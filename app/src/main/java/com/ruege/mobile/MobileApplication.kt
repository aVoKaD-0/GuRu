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
import com.ruege.mobile.data.local.preferences.AppPreferences
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class MobileApplication : Application(), Configuration.Provider {
    companion object {
        private const val TAG = "MobileApplication"
        private const val DB_VERSION = 12
        private const val FORCE_DB_RESET = false
    }

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

        Timber.plant(Timber.DebugTree())

        progressSyncRepository.initialize()
        Log.d(TAG, "ProgressSyncRepository.initialize() вызван в MobileApplication")

        progressRepository.initialize()
        Log.d(TAG, "ProgressRepository.initialize() вызван в MobileApplication")

        val preferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentDbVersion = preferences.getInt("db_version", 0)
        
        if (currentDbVersion < DB_VERSION || BuildConfig.DEBUG && FORCE_DB_RESET) {
            DBResetHelper.resetDatabase(this)
            preferences.edit().putInt("db_version", DB_VERSION).apply()
        }
        
        val appPreferences = AppPreferences(this)
        if (appPreferences.isDarkTheme()) {
            Log.d(TAG, "с")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            Log.d(TAG, "тема стоит светлая")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        Log.d(TAG, "MobileApplication onCreate complete")
    }
} 