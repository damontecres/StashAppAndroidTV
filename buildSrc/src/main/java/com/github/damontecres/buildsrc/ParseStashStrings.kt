package com.github.damontecres.buildsrc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * A gradle task that parses the server's localized messages into Android string resources
 */
abstract class ParseStashStrings : DefaultTask() {
    @get:InputDirectory
    lateinit var sourceDirectory: File

    @get:OutputDirectory
    lateinit var outputDirectory: File

    @TaskAction
    fun parse() {
        val mainFile = (sourceDirectory / MAIN_FILE).absoluteFile
        val mainDest = (outputDirectory / "values" / DEST_FILENAME).absoluteFile

        // Parse the main file to get a list of all available keys
        val allowedKeys = convertFile(mainFile, mainDest)

        sourceDirectory.listFiles { dir, name -> name != MAIN_FILE && name.endsWith(".json") }
            ?.forEach { file ->
                // Convert the language into Android's form
                val lang =
                    file.name
                        .replace(".json", "")
                        .replace("-", "+")
                        .replace("_", "+")
                val destFile = outputDirectory / "values-b+$lang" / DEST_FILENAME
                convertFile(file, destFile, allowedKeys)
            }
    }

    /**
     * Convert a source JSON file into Android string XML
     */
    private fun convertFile(
        sourceFile: File,
        destFile: File,
        allowedKeys: Set<String> = setOf(),
    ): MutableSet<String> {
        val collectedKeys = mutableSetOf<String>()
        val root = Json.parseToJsonElement(sourceFile.readText())
        // All keys will be prefixed with "stashapp"
        val entries = parseDictionary(root.jsonObject, listOf("stashapp"))
        destFile.parentFile.mkdirs()
        destFile.printWriter().use { out ->
            out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            out.println("<resources>")
            entries.forEach {
                if (allowedKeys.isEmpty() || allowedKeys.contains(it.key)) {
                    out.print("    ")
                    out.println(it.xml)
                    collectedKeys.add(it.key)
                }
            }
            out.println("</resources>")
        }
        return collectedKeys
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
    private fun parseDictionary(
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
                throw IllegalStateException("$keys + $key = $value")
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

    data class Entry(val key: String, val value: String, val formatted: Boolean) {
        val xml get() = "<string name=\"$key\" formatted=\"${formatted}\">$value</string>"
    }

    companion object {
        // The main file (which has all of the possible keys) is the en-GB file
        const val MAIN_FILE = "en-GB.json"
        const val DEST_FILENAME = "stash_strings.xml"
        val PARAM_REGEX = Regex("\\{\\w+}")
    }
}

private operator fun File.div(file: String): File {
    return File(this, file)
}
