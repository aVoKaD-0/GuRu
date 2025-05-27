package com.ruege.mobile.di

import android.content.Context
import com.ruege.mobile.data.local.dao.ContentDao
import com.ruege.mobile.data.local.dao.TaskDao
import com.ruege.mobile.data.network.api.EssayApiService
import com.ruege.mobile.data.network.api.TaskApiService
import com.ruege.mobile.data.network.api.TheoryApiService
import com.ruege.mobile.data.repository.ContentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideApplicationCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideContentRepository(
        contentDao: ContentDao,
        taskDao: TaskDao,
        theoryApiService: TheoryApiService,
        taskApiService: TaskApiService,
        essayApiService: EssayApiService,
        coroutineScope: CoroutineScope
    ): ContentRepository {
        return ContentRepository(
            contentDao,
            taskDao,
            theoryApiService,
            taskApiService,
            essayApiService,
            coroutineScope
        )
    }
} 