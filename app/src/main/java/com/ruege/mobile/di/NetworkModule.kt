package com.ruege.mobile.di

import com.ruege.mobile.data.network.adapter.DateAdapter
import com.ruege.mobile.data.network.api.AuthApiService
import com.ruege.mobile.data.network.api.NewsApiService
import com.ruege.mobile.data.network.api.ProgressApiService
import com.ruege.mobile.data.network.api.TaskApiService
import com.ruege.mobile.data.network.api.TheoryApiService
import com.ruege.mobile.data.network.api.VariantApiService
import com.ruege.mobile.data.network.api.EssayApiService
import com.ruege.mobile.auth.AuthInterceptor
import com.ruege.mobile.auth.TokenAuthenticator
import com.ruege.mobile.data.network.api.PracticeApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import timber.log.Timber

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val BASE_URL = "http://46.8.232.191:80/api/v1/"

    private const val DEBUG = true

    @Provides
    @Singleton
    @Named("BaseUrl")
    fun provideBaseUrl(): String = BASE_URL

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        Timber.d("provideHttpLoggingInterceptor: Creating HttpLoggingInterceptor. DEBUG is $DEBUG")
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            Timber.d("HttpLoggingInterceptor level set to ")
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenAuthenticator: TokenAuthenticator,
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        Timber.d("provideOkHttpClient: START Creating OkHttpClient.")
        Timber.d("provideOkHttpClient: Using loggingInterceptor: $loggingInterceptor")
        Timber.d("provideOkHttpClient: Using authInterceptor: $authInterceptor")
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .build()
        Timber.d("provideOkHttpClient: FINISHED Creating OkHttpClient (AuthInterceptor ENABLED, TokenAuthenticator TEMPORARILY DISABLED): $client")
        return client
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        Timber.d("provideMoshi: Creating Moshi instance.")
        return Moshi.Builder()
            .add(DateAdapter()) 
            .add(KotlinJsonAdapterFactory()) 
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        Timber.d("provideRetrofit: START Creating Retrofit with OkHttpClient: $okHttpClient and Moshi: $moshi")
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        Timber.d("provideRetrofit: FINISHED Creating Retrofit: $retrofit with base URL: $BASE_URL")
        return retrofit
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService {
        Timber.d("provideAuthApiService: Creating AuthApiService instance.")
        return retrofit.create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideNewsApiService(retrofit: Retrofit): NewsApiService {
        Timber.d("provideNewsApiService: Creating NewsApiService instance.")
        return retrofit.create(NewsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProgressApiService(retrofit: Retrofit): ProgressApiService {
        Timber.d("provideProgressApiService: Creating ProgressApiService instance.")
        return retrofit.create(ProgressApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTaskApiService(retrofit: Retrofit): TaskApiService {
        Timber.d("provideTaskApiService: Creating TaskApiService instance.")
        return retrofit.create(TaskApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTheoryApiService(retrofit: Retrofit): TheoryApiService {
        Timber.d("provideTheoryApiService: Creating TheoryApiService instance.")
        return retrofit.create(TheoryApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideVariantApiService(retrofit: Retrofit): VariantApiService {
        Timber.d("provideVariantApiService: Creating VariantApiService instance.")
        return retrofit.create(VariantApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideEssayApiService(retrofit: Retrofit): EssayApiService {
        Timber.d("provideEssayApiService: Creating EssayApiService instance.")
        return retrofit.create(EssayApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePracticeApiService(retrofit: Retrofit): PracticeApiService {
        Timber.d("providePracticeApiService: Creating PracticeApiService instance.")
        return retrofit.create(PracticeApiService::class.java)
    }
} 