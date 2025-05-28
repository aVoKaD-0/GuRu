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

@Module
@InstallIn(SingletonComponent::class)
object ShpargalkaModule {
    
    @Provides
    @Singleton
    fun provideShpargalkiApiService(retrofit: Retrofit): ShpargalkiApiService {
        return retrofit.create(ShpargalkiApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideShpargalkaRepository(
        shpargalkiApiService: ShpargalkiApiService,
        contentDao: ContentDao,
        categoryDao: CategoryDao,
        appDatabase: AppDatabase,
        @ApplicationContext context: Context
    ): ShpargalkaRepository {
        return ShpargalkaRepository(shpargalkiApiService, contentDao, categoryDao, appDatabase, context)
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
} 