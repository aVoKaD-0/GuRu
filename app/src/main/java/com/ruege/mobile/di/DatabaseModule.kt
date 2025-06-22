package com.ruege.mobile.di

import android.content.Context
import androidx.room.Room
import com.ruege.mobile.data.local.AppDatabase
import com.ruege.mobile.data.local.dao.CategoryDao
import com.ruege.mobile.data.local.dao.ContentDao
import com.ruege.mobile.data.local.dao.NewsDao
import com.ruege.mobile.data.local.dao.PracticeAttemptDao
import com.ruege.mobile.data.local.dao.PracticeStatisticsDao
import com.ruege.mobile.data.local.dao.ProgressDao
import com.ruege.mobile.data.local.dao.ProgressSyncQueueDao
import com.ruege.mobile.data.local.dao.TaskDao
import com.ruege.mobile.data.local.dao.UserDao
import com.ruege.mobile.data.local.dao.UserTaskAttemptDao
import com.ruege.mobile.data.local.dao.TaskOptionDao
import com.ruege.mobile.data.local.dao.VariantDao
import com.ruege.mobile.data.local.dao.VariantSharedTextDao
import com.ruege.mobile.data.local.dao.VariantTaskDao
import com.ruege.mobile.data.local.dao.DownloadedTheoryDao
import com.ruege.mobile.data.local.dao.SyncQueueDao
import com.ruege.mobile.data.local.dao.UserVariantTaskAnswerDao
import com.ruege.mobile.data.local.dao.VariantTaskOptionDao
import com.ruege.mobile.data.network.api.ProgressApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.ruege.mobile.data.local.dao.ShpargalkaDao

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return AppDatabase.getInstance(appContext)
    }

    @Provides
    fun provideNewsDao(appDatabase: AppDatabase): NewsDao {
        return appDatabase.newsDao()
    }

    @Provides
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    @Provides
    fun provideCategoryDao(appDatabase: AppDatabase): CategoryDao {
        return appDatabase.categoryDao()
    }

    @Provides
    @Singleton
    fun provideContentDao(appDatabase: AppDatabase): ContentDao {
        return appDatabase.contentDao()
    }

    @Provides
    fun provideProgressDao(appDatabase: AppDatabase): ProgressDao {
        return appDatabase.progressDao()
    }
    
    @Provides
    fun provideProgressSyncQueueDao(appDatabase: AppDatabase): ProgressSyncQueueDao {
        return appDatabase.progressSyncQueueDao()
    }

    @Provides
    @Singleton
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
        return appDatabase.taskDao()
    }

    @Provides
    fun provideUserTaskAttemptDao(appDatabase: AppDatabase): UserTaskAttemptDao {
        return appDatabase.userTaskAttemptDao()
    }
    
    @Provides
    fun providePracticeAttemptDao(appDatabase: AppDatabase): PracticeAttemptDao {
        return appDatabase.practiceAttemptDao()
    }
    
    @Provides
    fun providePracticeStatisticsDao(appDatabase: AppDatabase): PracticeStatisticsDao {
        return appDatabase.practiceStatisticsDao()
    }
    
    @Provides
    fun provideTaskOptionDao(appDatabase: AppDatabase): TaskOptionDao {
        return appDatabase.taskOptionDao()
    }

    @Provides
    @Singleton
    fun provideVariantDao(appDatabase: AppDatabase): VariantDao {
        return appDatabase.variantDao()
    }

    @Provides
    @Singleton
    fun provideVariantSharedTextDao(appDatabase: AppDatabase): VariantSharedTextDao {
        return appDatabase.variantSharedTextDao()
    }

    @Provides
    @Singleton
    fun provideVariantTaskDao(appDatabase: AppDatabase): VariantTaskDao {
        return appDatabase.variantTaskDao()
    }

    @Provides
    @Singleton
    fun provideUserVariantTaskAnswerDao(appDatabase: AppDatabase): UserVariantTaskAnswerDao {
        return appDatabase.userVariantTaskAnswerDao()
    }

    @Provides
    @Singleton
    fun provideVariantTaskOptionDao(appDatabase: AppDatabase): VariantTaskOptionDao {
        return appDatabase.variantTaskOptionDao()
    }

    @Provides
    fun provideDownloadedTheoryDao(appDatabase: AppDatabase): DownloadedTheoryDao {
        return appDatabase.downloadedTheoryDao()
    }

    @Provides
    fun provideSyncQueueDao(appDatabase: AppDatabase): SyncQueueDao {
        return appDatabase.syncQueueDao()
    }

    @Provides
    fun provideShpargalkaDao(appDatabase: AppDatabase): ShpargalkaDao {
        return appDatabase.shpargalkaDao()
    }
} 