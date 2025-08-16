package com.github.damontecres.stashapp.util

import android.util.Log
import dev.b3nedikt.restring.Restring
import dev.b3nedikt.restring.toMutableRepository
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val PARAM_REGEX = Regex("\\{\\w+\\}")

fun Restring.clear() {
    this.stringRepository
        .toMutableRepository()
        .strings[this.locale]
        ?.clear()
}

/**
 * Recursively convert messages into a list of [Entry] objects
 *
 * Nested JSON objects will stack up the keys into a list.
 * This allows for uniquely identifying a message ID in the flat format that Android uses
 *
 * @param data the JSON dictionary object
 * @param keys the keys stacked so far
 */
fun parseDictionary(
    data: JsonObject,
    keys: List<String>,
): List<Entry> {
    val result = mutableListOf<Entry>()
    data.keys.map { key ->
        val newKeys = keys.toMutableList()
        newKeys.add(key)
        val value = data[key]!!
        if (value is JsonObject) {
            result += parseDictionary(value, newKeys)
        } else if (value is JsonPrimitive && value.isString) {
            result += createEntry(newKeys, value.content)
        } else {
            Log.w("LocaleOverride", "Unexpected value for $keys + $key: $value")
        }
    }
    return result
}

private fun createEntry(
    keys: List<String>,
    value: String,
): Entry {
    val key = keys.joinToString("_").replace("-", "_")
    // Android doesn't use full XML escapes, so just replacing a few characters is sufficient
    var escaped = value.replace("&", "&amp;")
    escaped = escaped.replace(">", "&gt;")
    escaped = escaped.replace("<", "&lt;")
    escaped = escaped.replace("'", "\\'")
    // Now replace the parameters in the message values with the equivalent Android format
    val matches = PARAM_REGEX.findAll(escaped)
    matches.forEachIndexed { index, matchResult ->
        escaped = escaped.replaceFirst(matchResult.value, "%${index + 1}\$s")
    }
    return Entry(key, escaped, matches.iterator().hasNext())
}

data class Entry(
    val key: String,
    val value: String,
    val formatted: Boolean,
) {
    val xml get() = "<string name=\"$key\" formatted=\"${formatted}\">$value</string>"
}
