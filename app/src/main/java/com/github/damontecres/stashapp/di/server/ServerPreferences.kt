package com.github.damontecres.stashapp.di.server

import android.content.Context
import co.touchlab.kermit.Logger
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.ConfigurationQuery
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.PageFilterKey
import com.github.damontecres.stashapp.util.ServerPreferences.Companion.PREF_MINIMUM_PLAY_PERCENT
import com.github.damontecres.stashapp.util.ServerPreferences.Companion.PREF_TRACK_ACTIVITY
import com.github.damontecres.stashapp.util.Version
import com.github.damontecres.stashapp.util.clear
import com.github.damontecres.stashapp.util.getCaseInsensitive
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.parseDictionary
import com.github.damontecres.stashapp.util.plugin.CompanionPlugin
import dev.b3nedikt.restring.Restring
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.util.EnumSet

data class ServerPreferences(
    val version: Version = Version(0, 0, 0),
    val companionPluginVersion: Version? = null,
    val menuItems: EnumSet<DataType> = EnumSet.allOf(DataType::class.java),
//    val uiConfiguration: Map<*, *> = emptyMap(),
    val trackActivity: Boolean = true,
    val showStudioAsText: Boolean = false,
    val minimumPlayPercent: Int = 0,
    val ratingsAsStars: Boolean = true,
    val starPrecision: Float = 1f,
    val alwaysStartFromBeginning: Boolean = false,
    val abbreviateCounters: Boolean = false,
    val sfwMode: Boolean = false,
    val customLocalesEnabled: Boolean = false,
    val customLocales: String? = null,
    val defaultFilters: Map<DataType, FilterArgs> = emptyMap(),
    val defaultPageFilters: Map<PageFilterKey, FilterArgs> = emptyMap(),
    val frontPageContent: List<Map<String, *>> = emptyList(),
    val scanConfig: ConfigurationQuery.Scan =
        ConfigurationQuery.Scan(
            scanGenerateClipPreviews = false,
            scanGenerateCovers = false,
            scanGenerateImagePreviews = false,
            scanGeneratePhashes = false,
            scanGeneratePreviews = false,
            scanGenerateSprites = false,
            scanGenerateThumbnails = false,
        ),
    val generateConfig: ConfigurationQuery.Generate =
        ConfigurationQuery.Generate(
            clipPreviews = false,
            covers = false,
            imagePreviews = false,
            interactiveHeatmapsSpeeds = false,
            markerImagePreviews = false,
            markers = false,
            markerScreenshots = false,
            phashes = false,
            previews = false,
            sprites = false,
            transcodes = false,
            imageThumbnails = false,
        ),
) {
    val companionPluginInstalled: Boolean get() = companionPluginVersion != null

    companion object {
        fun createServerPreferences(config: ConfigurationQuery.Data): ServerPreferences {
            val serverVersion = Version.tryFromString(config.version.version) ?: Version(0, 0, 0)
            Logger.i { "updatePreferences for server version $serverVersion" }

            val companionPluginVersion =
                config.plugins
                    ?.firstOrNull { it.id == CompanionPlugin.PLUGIN_ID }
                    ?.version
                    ?.let { Version.tryFromString(it) }
            val menuItems =
                config.configuration.`interface`.menuItems
                    ?.map(String::lowercase)
                    ?.mapNotNull {
                        when (it) {
                            "scenes" -> DataType.SCENE
                            "groups", "movies" -> DataType.GROUP
                            "markers" -> DataType.MARKER
                            "performers" -> DataType.PERFORMER
                            "studios" -> DataType.STUDIO
                            "tags" -> DataType.TAG
                            "images" -> DataType.IMAGE
                            "galleries" -> DataType.GALLERY
                            else -> null
                        }
                    }?.let { EnumSet.copyOf(it) } ?: EnumSet.allOf(DataType::class.java)
            val showStudioAsText = config.configuration.`interface`.showStudioAsText ?: false
            val customLocalesEnabled =
                config.configuration.`interface`.customLocalesEnabled == true &&
                    config.configuration.`interface`.customLocales
                        .isNotNullOrBlank()
            val customLocales = config.configuration.`interface`.customLocales
            val sfwMode = config.configuration.`interface`.sfwContentMode

            var serverPreferences =
                ServerPreferences(
                    version = serverVersion,
                    companionPluginVersion = companionPluginVersion,
                    menuItems = menuItems,
                    showStudioAsText = showStudioAsText,
                    customLocalesEnabled = customLocalesEnabled,
                    customLocales = customLocales,
                    sfwMode = sfwMode,
                )

            val ui = config.configuration.ui
            if (ui !is Map<*, *>) {
                Logger.w { "config.configuration.ui is not a map" }
            } else {
                val taskDefaults = ui.getCaseInsensitive("taskDefaults") as Map<String, *>?
                try {
                    val scanConfig = parseScan(config.configuration.defaults.scan, taskDefaults)
                    serverPreferences = serverPreferences.copy(scanConfig = scanConfig)
                } catch (ex: Exception) {
                    Logger.e(ex) { "Exception during scan parsing" }
                }

                try {
                    val generateConfig = parseGenerate(config.configuration.defaults.generate, taskDefaults)
                    serverPreferences = serverPreferences.copy(generateConfig = generateConfig)
                } catch (ex: Exception) {
                    Logger.e(ex) { "Exception during generate parsing" }
                }

                serverPreferences = readUIConfig(serverPreferences, ui)
            }
            return serverPreferences
        }

        private fun readUIConfig(
            serverPreferences: ServerPreferences,
            ui: Map<*, *>,
        ): ServerPreferences {
            val frontPageContent = ui.getCaseInsensitive("frontPageContent") as? List<Map<String, *>>
            var serverPreferences = serverPreferences
            val trackActivity =
                ui
                    .getCaseInsensitive(
                        PREF_TRACK_ACTIVITY,
                    )?.toString()
                    ?.toBoolean() ?: if (serverPreferences.version.isAtLeast(Version.V0_26_0)) {
                    // If server is >=0.26.0 and doesn't provide a value, default to true
                    // See https://github.com/stashapp/stash/pull/4710
                    true
                } else {
                    // Server <0.26.0 default to false
                    false
                }

            val minimumPlayPercent = ui.getCaseInsensitive(PREF_MINIMUM_PLAY_PERCENT)?.toString()?.toIntOrNull() ?: 0
            val ratingSystemOptionsRaw = ui.getCaseInsensitive("ratingSystemOptions")

            try {
                val ratingSystemOptions = ratingSystemOptionsRaw as Map<String, String>?
                val ratingType = ratingSystemOptions?.getCaseInsensitive("type") ?: "stars"
                val starPrecision =
                    when (
                        ratingSystemOptions?.getCaseInsensitive("starPrecision")?.lowercase()
                    ) {
                        "full" -> 1.0f
                        "half" -> 0.5f
                        "quarter" -> 0.25f
                        "tenth" -> 0.1f
                        else -> 1.0f
                    }
                serverPreferences =
                    serverPreferences.copy(
                        ratingsAsStars = ratingType == "stars",
                        starPrecision = starPrecision,
                    )
            } catch (ex: Exception) {
                Logger.e(ex) { "Exception parsing ratingSystemOptions: $ratingSystemOptionsRaw" }
            }
            val alwaysStartFromBeginning = ui.getCaseInsensitive("alwaysStartFromBeginning")?.toString()?.toBoolean() ?: false
            val abbreviateCounters = ui.getCaseInsensitive("abbreviateCounters") as Boolean? ?: false

            val defaultFilters = ui.getCaseInsensitive("defaultFilters") as Map<String, *>?
            val filterParser = FilterParser(serverPreferences.version)
            val filters =
                DataType.entries.associateWith { dataType ->
                    val filterMap =
                        defaultFilters?.getCaseInsensitive(dataType.filterMode.name) as Map<String, *>?
                    val filter =
                        if (filterMap != null) {
                            try {
                                filterParser.convertFilterMap(dataType, filterMap)
                            } catch (ex: Exception) {
                                Logger.w(ex) { "default filter parse error for $dataType" }
                                FilterArgs(dataType)
                            }
                        } else {
                            FilterArgs(dataType)
                        }
                    filter
                }

            val pageFilters =
                PageFilterKey.entries.associateWith { key ->
                    val filterMap =
                        defaultFilters?.getCaseInsensitive(key.prefKey) as Map<String, *>?
                    if (filterMap != null) {
                        try {
                            filterParser.convertFilterMap(key.dataType, filterMap, false)
                        } catch (ex: Exception) {
                            Logger.w(ex) { "default filter parse error for $key" }
                            FilterArgs(key.dataType)
                        }
                    } else {
                        FilterArgs(key.dataType)
                    }
                }

            return serverPreferences.copy(
                frontPageContent = frontPageContent.orEmpty(),
                trackActivity = trackActivity,
                minimumPlayPercent = minimumPlayPercent,
                alwaysStartFromBeginning = alwaysStartFromBeginning,
                abbreviateCounters = abbreviateCounters,
                defaultFilters = filters,
                defaultPageFilters = pageFilters,
            )
        }

        private fun parseScan(
            defaultScan: ConfigurationQuery.Scan?,
            uiTaskDefaults: Map<String, *>?,
        ): ConfigurationQuery.Scan {
            val scan = uiTaskDefaults?.getCaseInsensitive("scan") as Map<String, *>?
            val scanGenerateCovers =
                scan.getBoolean("scanGenerateCovers", defaultScan?.scanGenerateCovers, false)
            val scanGeneratePreviews =
                scan.getBoolean("scanGeneratePreviews", defaultScan?.scanGeneratePreviews, false)
            val scanGenerateImagePreviews =
                scan.getBoolean(
                    "scanGenerateImagePreviews",
                    defaultScan?.scanGenerateImagePreviews,
                    false,
                )
            val scanGenerateSprites =
                scan.getBoolean("scanGenerateSprites", defaultScan?.scanGenerateSprites, false)
            val scanGeneratePhashes =
                scan.getBoolean("scanGeneratePhashes", defaultScan?.scanGeneratePhashes, false)
            val scanGenerateThumbnails =
                scan.getBoolean(
                    "scanGenerateThumbnails",
                    defaultScan?.scanGenerateThumbnails,
                    false,
                )
            val scanGenerateClipPreviews =
                scan.getBoolean(
                    "scanGenerateClipPreviews",
                    defaultScan?.scanGenerateClipPreviews,
                    false,
                )
            return ConfigurationQuery.Scan(
                scanGenerateClipPreviews = scanGenerateClipPreviews,
                scanGenerateCovers = scanGenerateCovers,
                scanGenerateImagePreviews = scanGenerateImagePreviews,
                scanGeneratePhashes = scanGeneratePhashes,
                scanGeneratePreviews = scanGeneratePreviews,
                scanGenerateSprites = scanGenerateSprites,
                scanGenerateThumbnails = scanGenerateThumbnails,
            )
        }

        private fun parseGenerate(
            defaultGenerate: ConfigurationQuery.Generate?,
            uiTaskDefaults: Map<String, *>?,
        ): ConfigurationQuery.Generate {
            val generate = uiTaskDefaults?.getCaseInsensitive("generate") as Map<String, *>?
            val clipPreviews =
                generate.getBoolean("clipPreviews", defaultGenerate?.clipPreviews, false)
            val covers = generate.getBoolean("covers", defaultGenerate?.covers, false)
            val imagePreviews =
                generate.getBoolean("imagePreviews", defaultGenerate?.imagePreviews, false)
            val interactiveHeatmapsSpeeds =
                generate.getBoolean(
                    "interactiveHeatmapsSpeeds",
                    defaultGenerate?.interactiveHeatmapsSpeeds,
                    false,
                )
            val markerImagePreviews =
                generate.getBoolean(
                    "markerImagePreviews",
                    defaultGenerate?.markerImagePreviews,
                    false,
                )
            val markers = generate.getBoolean("markers", defaultGenerate?.markers, false)
            val markerScreenshots =
                generate.getBoolean("markerScreenshots", defaultGenerate?.markerScreenshots, false)
            val phashes = generate.getBoolean("phashes", defaultGenerate?.phashes, false)
            val previews = generate.getBoolean("previews", defaultGenerate?.previews, false)
            val sprites = generate.getBoolean("sprites", defaultGenerate?.sprites, false)
            val transcodes = generate.getBoolean("transcodes", defaultGenerate?.transcodes, false)
            val imageThumbnails =
                generate.getBoolean("imageThumbnails", defaultGenerate?.imageThumbnails, false)
            return ConfigurationQuery.Generate(
                clipPreviews = clipPreviews,
                covers = covers,
                imagePreviews = imagePreviews,
                interactiveHeatmapsSpeeds = interactiveHeatmapsSpeeds,
                markerImagePreviews = markerImagePreviews,
                markers = markers,
                markerScreenshots = markerScreenshots,
                phashes = phashes,
                previews = previews,
                sprites = sprites,
                transcodes = transcodes,
                imageThumbnails = imageThumbnails,
            )
        }

        private fun Map<String, *>?.getBoolean(
            key: String,
            defaultsValue: Boolean?,
            default: Boolean,
        ): Boolean = this?.getCaseInsensitive(key)?.toString()?.toBoolean() ?: defaultsValue ?: default
    }

    fun restring(context: Context) {
        val useRestring = sfwMode || (customLocalesEnabled && customLocales.isNotNullOrBlank())
        if (useRestring) {
            Restring.clear()
            try {
                if (sfwMode) {
                    Restring.putStrings(
                        Restring.locale,
                        mapOf(
                            "stashapp_stats_total_o_count" to
                                context.getString(R.string.stashapp_stats_total_o_count_sfw),
                            "stashapp_o_count" to
                                context.getString(R.string.stashapp_o_count_sfw),
                            "stashapp_last_o_at" to
                                context.getString(R.string.stashapp_last_o_at_sfw),
                        ),
                    )
                }
                if (customLocalesEnabled && customLocales.isNotNullOrBlank()) {
                    val root = Json.parseToJsonElement(customLocales)
                    if (root is JsonObject) {
                        val map =
                            parseDictionary(
                                root.jsonObject,
                                listOf("stashapp"),
                                false,
                            ).associate { it.key to it.value }
                        Restring.putStrings(Restring.locale, map)

                        if (root.containsKey("StashAppAndroidTV") && root.jsonObject["StashAppAndroidTV"] is JsonObject) {
                            val appMap =
                                parseDictionary(
                                    root.jsonObject["StashAppAndroidTV"]!!.jsonObject,
                                    listOf(),
                                    true,
                                ).associate { it.key to it.value }
                            Restring.putStrings(Restring.locale, appMap)
                        }
                    }
                }
            } catch (ex: Exception) {
                Logger.w(ex) { "Error parsing custom locales" }
                Restring.clear()
            }
        } else {
            Restring.clear()
        }
    }
}
