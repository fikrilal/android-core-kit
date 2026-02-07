package dev.fikril.androidcorekit.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.fikril.androidcorekit.BuildConfig
import dev.fikril.androidcorekit.config.ApiConfig
import dev.fikril.androidcorekit.core.common.AppDispatchers
import dev.fikril.androidcorekit.core.network.auth.AccessTokenProvider
import dev.fikril.androidcorekit.core.network.auth.AccessTokenRefresher
import dev.fikril.androidcorekit.core.network.auth.NoOpAccessTokenProvider
import dev.fikril.androidcorekit.core.network.auth.NoOpAccessTokenRefresher
import dev.fikril.androidcorekit.core.network.client.ApiBaseUrlProvider
import dev.fikril.androidcorekit.core.network.client.NetworkClientFactory
import dev.fikril.androidcorekit.core.network.client.RetrofitServiceFactory
import dev.fikril.androidcorekit.core.network.client.StaticApiBaseUrlProvider
import dev.fikril.androidcorekit.core.network.execution.NetworkCallExecutor
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

    @Provides
    @Singleton
    fun provideApiBaseUrlProvider(apiConfig: ApiConfig): ApiBaseUrlProvider =
        StaticApiBaseUrlProvider(
            baseUrl = apiConfig.baseUrl,
        )

    @Provides
    @Singleton
    fun provideAccessTokenProvider(): AccessTokenProvider = NoOpAccessTokenProvider

    @Provides
    @Singleton
    fun provideAccessTokenRefresher(): AccessTokenRefresher = NoOpAccessTokenRefresher

    @Provides
    @Singleton
    fun provideNetworkClientFactory(
        accessTokenProvider: AccessTokenProvider,
        accessTokenRefresher: AccessTokenRefresher,
    ): NetworkClientFactory =
        NetworkClientFactory(
            accessTokenProvider = accessTokenProvider,
            accessTokenRefresher = accessTokenRefresher,
            enableBasicLogging = BuildConfig.DEBUG,
        )

    @Provides
    @Singleton
    fun provideRetrofitServiceFactory(
        apiBaseUrlProvider: ApiBaseUrlProvider,
        networkClientFactory: NetworkClientFactory,
    ): RetrofitServiceFactory =
        RetrofitServiceFactory(
            baseUrlProvider = apiBaseUrlProvider,
            networkClientFactory = networkClientFactory,
        )

    @Provides
    @Singleton
    fun provideNetworkCallExecutor(appDispatchers: AppDispatchers): NetworkCallExecutor =
        NetworkCallExecutor(
            dispatchers = appDispatchers,
        )
}

private object DefaultAppDispatchers : AppDispatchers {
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val main: CoroutineDispatcher = Dispatchers.Main
}
