package dev.fikril.androidcorekit.core.network.model

object NetworkHeaders {
    const val AUTHORIZATION = "Authorization"
    const val REQUEST_ID = "X-Request-Id"
    const val IDEMPOTENCY_KEY = "Idempotency-Key"

    // Internal-only opt-out header removed before transport.
    const val REQUIRES_AUTH = "X-Requires-Auth"
}
