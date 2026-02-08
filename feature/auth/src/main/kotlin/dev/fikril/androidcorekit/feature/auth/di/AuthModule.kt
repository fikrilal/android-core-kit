package dev.fikril.androidcorekit.feature.auth.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.fikril.androidcorekit.feature.auth.data.remote.AuthRemoteDataSource
import dev.fikril.androidcorekit.feature.auth.data.remote.DefaultAuthRemoteDataSource
import dev.fikril.androidcorekit.feature.auth.data.repository.AuthRepositoryImpl
import dev.fikril.androidcorekit.feature.auth.domain.repository.AuthRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AuthModule {
    @Binds
    @Singleton
    fun bindAuthRemoteDataSource(impl: DefaultAuthRemoteDataSource): AuthRemoteDataSource

    @Binds
    @Singleton
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
