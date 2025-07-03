package com.ruege.mobile.di

import com.google.android.recaptcha.RecaptchaClient
import com.ruege.mobile.MobileApplication
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RecaptchaModule {

    @Provides
    @Singleton
    fun provideRecaptchaClient(): RecaptchaClient {
        return MobileApplication.recaptchaClient
    }
} 