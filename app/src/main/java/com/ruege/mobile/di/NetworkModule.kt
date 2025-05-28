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
    private const val BASE_URL = "http://46.8.232.191:80/api/v1/"

    private const val DEBUG = true

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        Log.d("NetworkModule", "provideHttpLoggingInterceptor: Creating HttpLoggingInterceptor. DEBUG is $DEBUG")
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
            Log.d("NetworkModule", "HttpLoggingInterceptor level set to BODY for testing.")
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenAuthenticator: TokenAuthenticator,
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        Log.d("NetworkModule", "provideOkHttpClient: START Creating OkHttpClient.")
        Log.d("NetworkModule", "provideOkHttpClient: Using loggingInterceptor: $loggingInterceptor")
        Log.d("NetworkModule", "provideOkHttpClient: Using authInterceptor: $authInterceptor")
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .build()
        Log.d("NetworkModule", "provideOkHttpClient: FINISHED Creating OkHttpClient (AuthInterceptor ENABLED, TokenAuthenticator TEMPORARILY DISABLED): $client")
        return client
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
        Log.d("NetworkModule", "provideRetrofit: START Creating Retrofit with OkHttpClient: $okHttpClient and Moshi: $moshi")
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        Log.d("NetworkModule", "provideRetrofit: FINISHED Creating Retrofit: $retrofit with base URL: $BASE_URL")
        return retrofit
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
} 