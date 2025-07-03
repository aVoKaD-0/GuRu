package com.ruege.mobile

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.Configuration
import com.google.android.recaptcha.Recaptcha
import com.google.android.recaptcha.RecaptchaClient
import com.ruege.mobile.data.local.DBResetHelper
import com.ruege.mobile.data.repository.ProgressRepository
import com.ruege.mobile.data.repository.ProgressSyncRepository
import com.ruege.mobile.data.local.preferences.AppPreferences
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class MobileApplication : Application(), Configuration.Provider {
    companion object {
        private const val TAG = "MobileApplication"
        private const val DB_VERSION = 12
        private const val FORCE_DB_RESET = false
        lateinit var recaptchaClient: RecaptchaClient
            private set
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

    override val workManagerConfiguration: Configuration
        get() = workConfig

    override fun onCreate() {
        super.onCreate()

        initializeRecaptchaClient()

        Timber.plant(Timber.DebugTree())

        progressSyncRepository.initialize()
        Timber.d("ProgressSyncRepository.initialize() вызван в MobileApplication")

        progressRepository.initialize()
        Timber.d("ProgressRepository.initialize() вызван в MobileApplication")

        val preferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val currentDbVersion = preferences.getInt("db_version", 0)
        
        if (currentDbVersion < DB_VERSION || BuildConfig.DEBUG && FORCE_DB_RESET) {
            DBResetHelper.resetDatabase(this)
            preferences.edit().putInt("db_version", DB_VERSION).apply()
        }
        
        val appPreferences = AppPreferences(this)
        if (appPreferences.isDarkTheme()) {
            Timber.d("с")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            Timber.d("тема стоит светлая")
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        Timber.d("MobileApplication onCreate complete")
    }

    private fun initializeRecaptchaClient() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                recaptchaClient = Recaptcha.fetchClient(this@MobileApplication, BuildConfig.RECAPTCHA_SITE_KEY)
            } catch (e: Exception) {
                Timber.e(e, "Error initializing reCAPTCHA client")
            }
        }
    }
} 