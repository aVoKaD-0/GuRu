package com.ruege.mobile.di;

import android.content.Context;
import android.util.Log;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;
import androidx.work.WorkerFactory;
import com.ruege.mobile.worker.ProgressSyncWorkerFactory;
// import androidx.work.WorkManager; // Больше не предоставляем WorkManager напрямую здесь
// import com.ruege.mobile.worker.CustomWorkerFactory; // Больше не используем
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class WorkManagerModule {
    private static final String TAG = "WorkManagerModule";
    
    @Provides
    @Singleton
    public Configuration provideWorkManagerConfiguration(ProgressSyncWorkerFactory progressSyncWorkerFactory) {
        Log.d(TAG, "Creating WorkManager configuration with custom ProgressSyncWorkerFactory");
        
        return new Configuration.Builder()
                .setWorkerFactory(progressSyncWorkerFactory) // Используем нашу фабрику вместо HiltWorkerFactory
                .setMinimumLoggingLevel(Log.DEBUG) // Используем логирование для отладки
                .build();
    }
    
    // WorkManager будет автоматически инициализирован с нашей конфигурацией,
    // благодаря использованию MobileApplication как Configuration.Provider
    // Нам не нужно явно вызывать WorkManager.initialize или WorkManager.getInstance здесь.
    // Просто внедряйте WorkManager там, где он нужен, через @Inject.
} 