package dev.fikril.androidcorekit.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.fikril.androidcorekit.BuildConfig
import dev.fikril.androidcorekit.config.ApiConfig
import dev.fikril.androidcorekit.core.common.AppDispatchers
import dev.fikril.androidcorekit.core.session.SessionManager
import dev.fikril.androidcorekit.session.InMemorySessionManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AppBindingsModule {
    @Binds
    @Singleton
    fun bindSessionManager(impl: InMemorySessionManager): SessionManager
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideApiConfig(): ApiConfig =
        ApiConfig(
            baseUrl = BuildConfig.BASE_URL,
        )

    @Provides
    @Singleton
    fun provideAppDispatchers(): AppDispatchers = DefaultAppDispatchers
}

private object DefaultAppDispatchers : AppDispatchers {
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val main: CoroutineDispatcher = Dispatchers.Main
}
