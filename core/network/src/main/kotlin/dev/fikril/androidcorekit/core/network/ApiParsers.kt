package dev.fikril.androidcorekit.core.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

typealias JsonParser<T> = (JsonElement) -> T
typealias MapParser<T> = (Map<String, Any?>) -> T

object NoData

val noDataParser: JsonParser<NoData> = { NoData }

fun <T> mapParser(parser: MapParser<T>): JsonParser<T> =
    { jsonElement ->
        parser(jsonElement.requireJsonObject().toAnyMap())
    }

internal fun JsonElement.requireJsonObject(): JsonObject =
    this as? JsonObject ?: error("Expected JSON object but got ${this::class.simpleName}.")

internal fun JsonObject.toAnyMap(): Map<String, Any?> = mapValues { (_, value) -> value.toAnyValue() }

private fun JsonElement.toAnyValue(): Any? =
    when (this) {
        JsonNull -> null
        is JsonObject -> toAnyMap()
        is JsonArray -> map { it.toAnyValue() }
        is JsonPrimitive -> {
            val primitiveContent = contentOrNull ?: toString()
            booleanOrNull?.let { return it }
            longOrNull?.let { return it }
            primitiveContent.toDoubleOrNull()?.let { return it }
            primitiveContent
        }
    }
