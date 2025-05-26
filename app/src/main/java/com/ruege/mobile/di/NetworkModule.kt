package com.ruege.mobile.di

import android.content.Context
import android.util.Log
import com.ruege.mobile.data.local.TokenManager
import com.ruege.mobile.data.network.adapter.DateAdapter
import com.ruege.mobile.data.network.api.AuthApiService
import com.ruege.mobile.data.network.api.NewsApiService
import com.ruege.mobile.data.network.api.ProgressApiService
import com.ruege.mobile.data.network.api.TaskApiService
import com.ruege.mobile.data.network.api.TheoryApiService
import com.ruege.mobile.data.network.api.VariantApiService
import com.ruege.mobile.data.network.api.EssayApiService
import com.ruege.mobile.data.network.interceptor.AuthInterceptor
import com.ruege.mobile.data.network.authenticator.TokenAuthenticator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Адрес для эмулятора Android, указывающий на localhost хост-машины
    // Убедись, что Nginx настроен на порт 80 и проксирует на бэкенд
    // private const val BASE_URL = "http://10.0.2.2:80/api/v1/"
    // Используем IP-адрес хост-машины в локальной сети
    private const val BASE_URL = "http://46.8.232.191:80/api/v1/"
    
    // Константа для дебага
    private const val DEBUG = true

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        Log.d("NetworkModule", "provideHttpLoggingInterceptor: Creating HttpLoggingInterceptor with level BODY (if DEBUG). DEBUG is $DEBUG")
        return HttpLoggingInterceptor().apply {
            level = if (DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenAuthenticator: TokenAuthenticator,
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        Log.d("NetworkModule", "provideOkHttpClient: Creating OkHttpClient with AuthInterceptor and TokenAuthenticator.")
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        Log.d("NetworkModule", "provideMoshi: Creating Moshi instance.")
        return Moshi.Builder()
            .add(DateAdapter()) 
            .add(KotlinJsonAdapterFactory()) 
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        Log.d("NetworkModule", "provideRetrofit: Creating Retrofit instance with base URL: $BASE_URL")
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        Log.d("NetworkModule", "provideAuthApiService: Creating AuthApiService instance.")
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNewsApiService(retrofit: Retrofit): NewsApiService {
        Log.d("NetworkModule", "provideNewsApiService: Creating NewsApiService instance.")
        return retrofit.create(NewsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProgressApiService(retrofit: Retrofit): ProgressApiService {
        Log.d("NetworkModule", "provideProgressApiService: Creating ProgressApiService instance.")
        return retrofit.create(ProgressApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTaskApiService(retrofit: Retrofit): TaskApiService {
        Log.d("NetworkModule", "provideTaskApiService: Creating TaskApiService instance.")
        return retrofit.create(TaskApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTheoryApiService(retrofit: Retrofit): TheoryApiService {
        Log.d("NetworkModule", "provideTheoryApiService: Creating TheoryApiService instance.")
        return retrofit.create(TheoryApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideVariantApiService(retrofit: Retrofit): VariantApiService {
        Log.d("NetworkModule", "provideVariantApiService: Creating VariantApiService instance.")
        return retrofit.create(VariantApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideEssayApiService(retrofit: Retrofit): EssayApiService {
        Log.d("NetworkModule", "provideEssayApiService: Creating EssayApiService instance.")
        return retrofit.create(EssayApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePracticeApiService(retrofit: Retrofit): com.ruege.mobile.data.network.api.PracticeApiService {
        Log.d("NetworkModule", "providePracticeApiService: Creating PracticeApiService instance.")
        return retrofit.create(com.ruege.mobile.data.network.api.PracticeApiService::class.java)
    }

    // AuthInterceptor зависит от TokenManager, который Hilt уже знает как создать (@Inject в конструкторе TokenManager)
    // Поэтому дополнительно предоставлять AuthInterceptor не нужно, Hilt сам его создаст.
    // Однако, мы его временно отключили в provideOkHttpClient
} 