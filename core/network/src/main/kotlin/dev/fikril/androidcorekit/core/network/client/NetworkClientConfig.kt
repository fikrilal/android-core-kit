package dev.fikril.androidcorekit.core.network.client

data class NetworkClientConfig(
    val connectTimeoutMillis: Long = 10_000L,
    val readTimeoutMillis: Long = 30_000L,
    val writeTimeoutMillis: Long = 30_000L,
    val callTimeoutMillis: Long = 60_000L,
    val maxIdleConnections: Int = 5,
    val keepAliveDurationMinutes: Long = 5,
    val securityConfig: NetworkSecurityConfig = NetworkSecurityConfig(),
)

data class NetworkRetryPolicy(
    val maxAttempts: Int = 3,
    val initialBackoffMillis: Long = 200L,
    val maxBackoffMillis: Long = 1_000L,
    val retryableStatusCodes: Set<Int> = DEFAULT_RETRYABLE_STATUS_CODES,
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1." }
        require(initialBackoffMillis >= 0L) { "initialBackoffMillis must be >= 0." }
        require(maxBackoffMillis >= initialBackoffMillis) {
            "maxBackoffMillis must be >= initialBackoffMillis."
        }
    }

    private companion object {
        val DEFAULT_RETRYABLE_STATUS_CODES = setOf(429, 500, 502, 503, 504)
    }
}
