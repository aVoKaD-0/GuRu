package com.ruege.mobile.worker;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.ListenableWorker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import com.ruege.mobile.data.local.dao.ProgressSyncQueueDao;
import com.ruege.mobile.data.network.api.ProgressApiService;
import com.ruege.mobile.data.repository.PracticeSyncRepository;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Фабрика для создания ProgressSyncWorker с внедрением зависимостей.
 */
@Singleton
public class ProgressSyncWorkerFactory extends WorkerFactory {
    private static final String TAG = "PrgSyncWorkerFactory";

    private final ProgressSyncQueueDao progressSyncQueueDao;
    private final ProgressApiService progressApiService;
    private final PracticeSyncRepository practiceSyncRepository;

    @Inject
    public ProgressSyncWorkerFactory(
            ProgressSyncQueueDao progressSyncQueueDao,
            ProgressApiService progressApiService,
            PracticeSyncRepository practiceSyncRepository) {
        this.progressSyncQueueDao = progressSyncQueueDao;
        this.progressApiService = progressApiService;
        this.practiceSyncRepository = practiceSyncRepository;
        
        // Логируем создание фабрики для отладки
        Log.d(TAG, "ProgressSyncWorkerFactory created with progressSyncQueueDao and progressApiService");
    }

    @Nullable
    @Override
    public ListenableWorker createWorker(
            @NonNull Context appContext,
            @NonNull String workerClassName,
            @NonNull WorkerParameters workerParameters) {
        
        try {
            Log.d(TAG, "Attempting to create worker: " + workerClassName);
            
            if (workerClassName.equals(ProgressSyncWorker.class.getName())) {
                Log.d(TAG, "Creating ProgressSyncWorker via factory");
                
                return new ProgressSyncWorker(
                    appContext,
                    workerParameters,
                    progressSyncQueueDao,
                    progressApiService,
                    practiceSyncRepository
                );
            } else {
                Log.d(TAG, "Using default creation for worker: " + workerClassName);
            }
            
            // Для других типов worker'ов возвращаем null, чтобы система создавала их стандартным способом
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error creating worker " + workerClassName, e);
            // При ошибке возвращаем null, чтобы система могла попробовать использовать стандартный механизм
            return null;
        }
    }
} 