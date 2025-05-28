package com.ruege.mobile.di

import com.ruege.mobile.data.repository.ShpargalkaRepository
import com.ruege.mobile.viewmodel.ShpargalkaViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    
    @Provides
    @ViewModelScoped
    fun provideShpargalkaViewModel(repository: ShpargalkaRepository): ShpargalkaViewModel {
        return ShpargalkaViewModel(repository)
    }
} 