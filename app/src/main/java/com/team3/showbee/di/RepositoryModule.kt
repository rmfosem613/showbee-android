package com.team3.showbee.di

import com.team3.showbee.data.repository.login.LogInRepository
import com.team3.showbee.data.repository.login.LogInRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindsLoginRepository(
        repositoryImpl: LogInRepositoryImpl
    ): LogInRepository
}