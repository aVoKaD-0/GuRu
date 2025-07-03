package com.ruege.mobile.di;


import androidx.work.Configuration;
import com.ruege.mobile.worker.ProgressSyncWorkerFactory;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;
import timber.log.Timber;

@Module
@InstallIn(SingletonComponent.class)
public class WorkManagerModule {
    private static final String TAG = "WorkManagerModule";
    
    @Provides
    @Singleton
    public Configuration provideWorkManagerConfiguration(ProgressSyncWorkerFactory progressSyncWorkerFactory) {
        Timber.d("Creating WorkManager configuration with custom ProgressSyncWorkerFactory");
        
        return new Configuration.Builder()
                .setWorkerFactory(progressSyncWorkerFactory)
                .build();
    }
} 