package com.ruege.mobile.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.ListenableWorker;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import com.ruege.mobile.data.local.dao.ProgressSyncQueueDao;
import com.ruege.mobile.data.network.api.ProgressApiService;
import com.ruege.mobile.data.network.api.PracticeApiService;
import com.ruege.mobile.data.repository.PracticeSyncRepository;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * Фабрика для создания ProgressSyncWorker с внедрением зависимостей.
 */
@Singleton
public class ProgressSyncWorkerFactory extends WorkerFactory {
    private static final String TAG = "PrgSyncWorkerFactory";

    private final ProgressSyncQueueDao progressSyncQueueDao;
    private final ProgressApiService progressApiService;
    private final PracticeApiService practiceApiService;
    private final PracticeSyncRepository practiceSyncRepository;

    @Inject
    public ProgressSyncWorkerFactory(
            ProgressSyncQueueDao progressSyncQueueDao,
            ProgressApiService progressApiService,
            PracticeApiService practiceApiService,
            PracticeSyncRepository practiceSyncRepository) {
        this.progressSyncQueueDao = progressSyncQueueDao;
        this.progressApiService = progressApiService;
        this.practiceApiService = practiceApiService;
        this.practiceSyncRepository = practiceSyncRepository;
        
        Timber.d("ProgressSyncWorkerFactory created with all api services");
    }

    @Nullable
    @Override
    public ListenableWorker createWorker(
            @NonNull Context appContext,
            @NonNull String workerClassName,
            @NonNull WorkerParameters workerParameters) {
        
        try {
            Timber.d("Attempting to create worker: " + workerClassName);
            
            if (workerClassName.equals(ProgressSyncWorker.class.getName())) {
                Timber.d("Creating ProgressSyncWorker via factory");
                
                return new ProgressSyncWorker(
                    appContext,
                    workerParameters,
                    progressSyncQueueDao,
                    progressApiService,
                    practiceApiService,
                    practiceSyncRepository
                );
            } else {
                Timber.d("Using default creation for worker: " + workerClassName);
            }
            
            return null;
        } catch (Exception e) {
            Timber.e(e, "Error creating worker " + workerClassName);
            return null;
        }
    }
} 