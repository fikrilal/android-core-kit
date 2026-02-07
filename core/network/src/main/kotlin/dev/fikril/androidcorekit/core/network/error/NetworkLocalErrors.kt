package dev.fikril.androidcorekit.core.network.error

object NetworkLocalStatus {
    const val NO_INTERNET = -1
    const val TIMEOUT = -2
    const val NETWORK_FAILURE = -3
    const val UNEXPECTED_FAILURE = -4
}

object NetworkLocalCode {
    const val NO_INTERNET = "NETWORK_UNAVAILABLE"
    const val TIMEOUT = "REQUEST_TIMEOUT"
    const val NETWORK_FAILURE = "NETWORK_ERROR"
    const val UNEXPECTED_FAILURE = "UNEXPECTED_NETWORK_ERROR"
    const val PARSER_ERROR = "PARSER_ERROR"
}
