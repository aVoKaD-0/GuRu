package com.ruege.mobile.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import timber.log.Timber
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Класс для управления настройками приложения с использованием SharedPreferences
 */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "AppPreferences"
    
    private val appPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_APP_SETTINGS,
        Context.MODE_PRIVATE
    )
    
    private val userPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_USER_SETTINGS,
        Context.MODE_PRIVATE
    )
    
    fun isDarkTheme(): Boolean {
        return appPreferences.getBoolean(KEY_DARK_THEME, false)
    }
    
    fun setSyncEnabled(enabled: Boolean) {
        userPreferences.edit().putBoolean(KEY_SYNC_ENABLED, enabled).apply()
    }

    fun setLastSyncTime(timestamp: Long) {
        userPreferences.edit().putLong(KEY_LAST_SYNC_TIME, timestamp).apply()
    }
    
    fun setNotificationsEnabled(enabled: Boolean) {
        userPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun setAutoLoadContent(enabled: Boolean) {
        userPreferences.edit().putBoolean(KEY_AUTO_LOAD_CONTENT, enabled).apply()
    }
    
    fun clearUserPreferences() {
        try {
            userPreferences.edit().clear().apply()
            Timber.d("Пользовательские настройки очищены")
            
            setSyncEnabled(true)
            setNotificationsEnabled(true)
            setAutoLoadContent(true)
            setLastSyncTime(0)
        } catch (e: Exception) {
            Timber.d("Ошибка при очистке пользовательских настроек", e)
        }
    }
    
    companion object {
        private const val PREFS_APP_SETTINGS = "app_settings"
        private const val PREFS_USER_SETTINGS = "user_settings"
        
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_LAST_SYNC_TIME = "last_sync_time"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_AUTO_LOAD_CONTENT = "auto_load_content"
        private const val KEY_FIRST_LAUNCH = "first_launch"
    }
} 