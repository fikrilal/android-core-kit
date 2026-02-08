package dev.fikril.androidcorekit.core.network.serialization

import kotlinx.serialization.json.Json

fun defaultNetworkJson(): Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
