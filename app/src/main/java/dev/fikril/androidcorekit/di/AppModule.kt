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
import dev.fikril.androidcorekit.core.network.client.ApiBaseUrlProvider
import dev.fikril.androidcorekit.core.network.client.NetworkClientConfig
import dev.fikril.androidcorekit.core.network.client.NetworkClientFactory
import dev.fikril.androidcorekit.core.network.client.NetworkRetryPolicy
import dev.fikril.androidcorekit.core.network.client.RetrofitServiceFactory
import dev.fikril.androidcorekit.core.network.client.StaticApiBaseUrlProvider
import dev.fikril.androidcorekit.core.network.execution.NetworkCallExecutor
import dev.fikril.androidcorekit.core.network.telemetry.NetworkTelemetryObserver
import dev.fikril.androidcorekit.core.session.SessionManager
import dev.fikril.androidcorekit.network.LoggingNetworkTelemetryObserver
import dev.fikril.androidcorekit.network.NetworkSecurityPolicyFactory
import dev.fikril.androidcorekit.session.AndroidKeystoreSessionCrypto
import dev.fikril.androidcorekit.session.BackendAccessTokenRefresher
import dev.fikril.androidcorekit.session.DataStoreEncryptedSessionStore
import dev.fikril.androidcorekit.session.PersistedSessionManager
import dev.fikril.androidcorekit.session.SessionAccessTokenProvider
import dev.fikril.androidcorekit.session.SessionCrypto
import dev.fikril.androidcorekit.session.SessionStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AppBindingsModule {
    @Binds
    @Singleton
    fun bindSessionManager(impl: PersistedSessionManager): SessionManager

    @Binds
    @Singleton
    fun bindSessionStore(impl: DataStoreEncryptedSessionStore): SessionStore

    @Binds
    @Singleton
    fun bindSessionCrypto(impl: AndroidKeystoreSessionCrypto): SessionCrypto
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
    fun provideNetworkClientConfig(
        apiConfig: ApiConfig,
        networkSecurityPolicyFactory: NetworkSecurityPolicyFactory,
    ): NetworkClientConfig =
        NetworkClientConfig(
            securityConfig =
                networkSecurityPolicyFactory.create(
                    baseUrl = apiConfig.baseUrl,
                    isProd = BuildConfig.FLAVOR == "prod",
                    primaryPin = BuildConfig.CERT_PIN_PRIMARY,
                    backupPin = BuildConfig.CERT_PIN_BACKUP,
                ),
        )

    @Provides
    @Singleton
    fun provideNetworkRetryPolicy(): NetworkRetryPolicy = NetworkRetryPolicy()

    @Provides
    @Singleton
    fun provideNetworkTelemetryObserver(observer: LoggingNetworkTelemetryObserver): NetworkTelemetryObserver = observer

    @Provides
    @Singleton
    fun provideAccessTokenProvider(provider: SessionAccessTokenProvider): AccessTokenProvider = provider

    @Provides
    @Singleton
    fun provideAccessTokenRefresher(refresher: BackendAccessTokenRefresher): AccessTokenRefresher = refresher

    @Provides
    @Singleton
    fun provideNetworkClientFactory(
        accessTokenProvider: AccessTokenProvider,
        accessTokenRefresher: AccessTokenRefresher,
        networkClientConfig: NetworkClientConfig,
        networkRetryPolicy: NetworkRetryPolicy,
        networkTelemetryObserver: NetworkTelemetryObserver,
    ): NetworkClientFactory =
        NetworkClientFactory(
            accessTokenProvider = accessTokenProvider,
            accessTokenRefresher = accessTokenRefresher,
            enableBasicLogging = BuildConfig.DEBUG,
            clientConfig = networkClientConfig,
            retryPolicy = networkRetryPolicy,
            telemetryObserver = networkTelemetryObserver,
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
