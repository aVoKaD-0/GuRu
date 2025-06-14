package com.ruege.mobile.di

import android.content.Context
import com.ruege.mobile.data.local.AppDatabase
import com.ruege.mobile.data.local.dao.CategoryDao
import com.ruege.mobile.data.local.dao.ContentDao
import com.ruege.mobile.data.local.dao.VariantDao
import com.ruege.mobile.data.local.dao.VariantSharedTextDao
import com.ruege.mobile.data.local.dao.VariantTaskDao
import com.ruege.mobile.data.local.dao.VariantTaskOptionDao
import com.ruege.mobile.data.local.dao.UserVariantTaskAnswerDao
import com.ruege.mobile.data.network.api.ShpargalkiApiService
import com.ruege.mobile.data.network.api.VariantApiService
import com.ruege.mobile.data.repository.ShpargalkaRepository
import com.ruege.mobile.data.repository.VariantRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton
import com.ruege.mobile.data.repository.ContentRepository
import com.ruege.mobile.data.local.dao.TaskDao
import com.ruege.mobile.data.local.dao.DownloadedTheoryDao
import com.ruege.mobile.data.local.dao.UserDao
import com.ruege.mobile.data.network.api.TaskApiService
import com.ruege.mobile.data.network.api.TheoryApiService
import com.ruege.mobile.data.network.api.EssayApiService
import kotlinx.coroutines.CoroutineScope
import com.ruege.mobile.data.repository.PracticeRepository
import com.ruege.mobile.data.repository.ProgressSyncRepository
import com.ruege.mobile.data.repository.PracticeSyncRepository
import com.ruege.mobile.data.repository.ProgressRepository
import com.ruege.mobile.data.repository.UserRepository
import com.ruege.mobile.data.local.dao.PracticeAttemptDao
import com.ruege.mobile.data.local.dao.PracticeStatisticsDao
import com.ruege.mobile.data.local.dao.ProgressDao
import com.ruege.mobile.data.local.dao.ProgressSyncQueueDao
import com.ruege.mobile.data.local.dao.ShpargalkaDao
import com.ruege.mobile.data.network.api.ProgressApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.ruege.mobile.data.local.dao.TaskTextDao
import com.ruege.mobile.data.network.api.PracticeApiService

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    
    @Provides
    @Singleton
    fun provideShpargalkiApiService(retrofit: Retrofit): ShpargalkiApiService {
        return retrofit.create(ShpargalkiApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideShpargalkaRepository(
        shpargalkiApiService: ShpargalkiApiService,
        shpargalkaDao: ShpargalkaDao,
        @ApplicationContext context: Context
    ): ShpargalkaRepository {
        return ShpargalkaRepository(shpargalkiApiService, shpargalkaDao, context)
    }

    @Provides
    @Singleton
    fun provideContentRepository(
        contentDao: ContentDao,
        taskDao: TaskDao,
        theoryApiService: TheoryApiService,
        taskApiService: TaskApiService,
        essayApiService: EssayApiService,
        downloadedTheoryDao: DownloadedTheoryDao,
        taskTextDao: TaskTextDao,
        externalScope: CoroutineScope,
        userDao: UserDao
    ): ContentRepository {
        return ContentRepository(
            contentDao,
            taskDao,
            theoryApiService,
            taskApiService,
            essayApiService,
            downloadedTheoryDao,
            taskTextDao,
            externalScope,
            userDao
        )
    }

    @Provides
    @Singleton
    fun provideVariantRepository(
        appDatabase: AppDatabase,
        variantApiService: VariantApiService,
        variantDao: VariantDao,
        variantSharedTextDao: VariantSharedTextDao,
        variantTaskDao: VariantTaskDao,
        variantTaskOptionDao: VariantTaskOptionDao,
        userVariantTaskAnswerDao: UserVariantTaskAnswerDao
    ): VariantRepository {
        return VariantRepository(
            appDatabase, 
            variantApiService, 
            variantDao,
            variantSharedTextDao,
            variantTaskDao,
            variantTaskOptionDao,
            userVariantTaskAnswerDao,
        )
    }

    @Provides
    @Singleton
    fun providePracticeRepository(
        practiceAttemptDao: PracticeAttemptDao,
        practiceStatisticsDao: PracticeStatisticsDao,
        taskDao: TaskDao,
    ): PracticeRepository {
        return PracticeRepository(practiceAttemptDao, practiceStatisticsDao, taskDao)
    }
    
    @Provides
    @Singleton
    fun provideProgressSyncRepository(
        @ApplicationContext context: Context,
        progressDao: ProgressDao,
        progressSyncQueueDao: ProgressSyncQueueDao,
        progressApiService: ProgressApiService,
        practiceApiService: PracticeApiService,
        contentDao: ContentDao,
        userDao: UserDao,
        practiceStatisticsDao: PracticeStatisticsDao,
        practiceSyncRepository: PracticeSyncRepository
    ): ProgressSyncRepository {
        return ProgressSyncRepository(context, progressDao, progressSyncQueueDao, progressApiService, practiceApiService, contentDao, userDao, practiceStatisticsDao, practiceSyncRepository)
    }
    
    @Provides
    @Singleton
    fun provideProgressRepository(
        progressDao: ProgressDao,
        progressApiService: ProgressApiService,
        progressSyncRepository: ProgressSyncRepository,
        contentDao: ContentDao,
        userDao: UserDao
    ): ProgressRepository {
        return ProgressRepository(progressDao, progressApiService, progressSyncRepository, contentDao, userDao)
    }
    
    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao
    ): UserRepository {
        return UserRepository(userDao)
    }
    
    @Provides
    fun provideTaskTextDao(appDatabase: AppDatabase): TaskTextDao {
        return appDatabase.taskTextDao();
    }
} 