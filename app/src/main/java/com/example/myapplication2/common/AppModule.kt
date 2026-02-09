package com.example.myapplication2.common

import com.example.myapplication2.wallet.SepoliaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSepoliaRepository(): SepoliaRepository = SepoliaRepository()
}

