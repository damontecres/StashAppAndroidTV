package com.github.damontecres.stashapp.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.ConfigurationQuery
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.plugin.CompanionPlugin

/**
 * Represents configuration that users have set server-side
 *
 * Configuration is loaded into a SharedPreferences and made available throughout the app
 */
class ServerPreferences(
    val server: StashServer,
) {
    val preferences: SharedPreferences =
        StashApplication.getApplication().getSharedPreferences(
            server.url.replace(Regex("[^\\w.]"), "_"),
            Context.MODE_PRIVATE,
        )

    val serverVersion: Version
        get() {
            return Version.fromString(
                preferences.getStringNotNull(
                    PREF_SERVER_VERSION,
                    "0.0.0",
                ),
            )
        }

    val trackActivity get() = preferences.getBoolean(PREF_TRACK_ACTIVITY, true)

    val showStudioAsText get() = preferences.getBoolean(PREF_INTERFACE_STUDIOS_AS_TEXT, false)

    val minimumPlayPercent get() = preferences.getInt(PREF_MINIMUM_PLAY_PERCENT, 20)

    val ratingsAsStars get() = preferences.getString(PREF_RATING_TYPE, "stars") == "stars"

    val alwaysStartFromBeginning get() = preferences.getBoolean(PREF_ALWAYS_START_BEGINNING, false)

    val abbreviateCounters get() = preferences.getBoolean(PREF_INTERFACE_ABBREV_COUNTERS, false)

    val companionPluginVersion
        get() =
            preferences.getString(
                PREF_COMPANION_PLUGIN_VERSION,
                null,
            )

    val companionPluginInstalled
        get() = companionPluginVersion != null

    private val _defaultFilters = mutableMapOf<DataType, FilterArgs>()
    val defaultFilters: Map<DataType, FilterArgs> = _defaultFilters
    private val defaultPageFilters = mutableMapOf<PageFilterKey, FilterArgs>()

    /**
     * Update the local preferences from the server configuration
     */
    fun updatePreferences(config: ConfigurationQuery.Data) {
        val serverVersion = Version.tryFromString(config.version.version)
        Log.i(TAG, "updatePreferences for server version $serverVersion, obj=$this")

        val companionPluginVersion =
            config.plugins?.firstOrNull { it.id == CompanionPlugin.PLUGIN_ID }?.version

        preferences.edit {
            putString(PREF_SERVER_VERSION, config.version.version)
            putString(PREF_COMPANION_PLUGIN_VERSION, companionPluginVersion)
        }
        if (config.configuration.ui is Map<*, *>) {
            val ui = config.configuration.ui as Map<String, *>
            preferences.edit(true) {
                ui.getCaseInsensitive(PREF_TRACK_ACTIVITY).also {
                    if (it != null) {
                        // Use a non-null value from server
                        putBoolean(PREF_TRACK_ACTIVITY, it.toString().toBoolean())
                    } else if (serverVersion != null && serverVersion.isAtLeast(Version.V0_26_0)) {
                        // If server is >=0.26.0 and doesn't provide a value, default to true
                        // See https://github.com/stashapp/stash/pull/4710
                        putBoolean(PREF_TRACK_ACTIVITY, true)
                    } else {
                        // Server <0.26.0 default to false
                        putBoolean(PREF_TRACK_ACTIVITY, false)
                    }
                }
                val minPlayPercent = ui.getCaseInsensitive(PREF_MINIMUM_PLAY_PERCENT)
                try {
                    putInt(PREF_MINIMUM_PLAY_PERCENT, minPlayPercent?.toString()?.toInt() ?: 20)
                } catch (ex: NumberFormatException) {
                    Log.w(TAG, "$PREF_MINIMUM_PLAY_PERCENT is not an integer: '$minPlayPercent'")
                }

                val ratingSystemOptionsRaw = ui.getCaseInsensitive("ratingSystemOptions")

                try {
                    val ratingSystemOptions = ratingSystemOptionsRaw as Map<String, String>?
                    val type = ratingSystemOptions?.getCaseInsensitive("type") ?: "stars"
                    val starPrecision =
                        when (
                            ratingSystemOptions?.getCaseInsensitive("starPrecision")?.lowercase()
                        ) {
                            "full" -> 1.0f
                            "half" -> 0.5f
                            "quarter" -> 0.25f
                            "tenth" -> 0.1f
                            else -> 0.5f
                        }
                    putString(PREF_RATING_TYPE, type)
                    putFloat(PREF_RATING_PRECISION, starPrecision)
                } catch (ex: Exception) {
                    Log.e(
                        TAG,
                        "Exception parsing ratingSystemOptions: $ratingSystemOptionsRaw",
                    )
                }

                val alwaysStartFromBeginning = ui.getCaseInsensitive("alwaysStartFromBeginning")
                putBoolean(
                    PREF_ALWAYS_START_BEGINNING,
                    alwaysStartFromBeginning?.toString()?.toBoolean() ?: false,
                )
                putBoolean(
                    PREF_INTERFACE_ABBREV_COUNTERS,
                    ui.getCaseInsensitive("abbreviateCounters") as Boolean? ?: false,
                )

                val taskDefaults = ui.getCaseInsensitive("taskDefaults") as Map<String, *>?
                try {
                    parseScan(config.configuration.defaults.scan, taskDefaults).invoke(this)
                    parseGenerate(config.configuration.defaults.generate, taskDefaults).invoke(this)
                } catch (ex: Exception) {
                    Log.e(TAG, "Exception during scan/generate parsing", ex)
                }

                val menuItems =
                    config.configuration.`interface`.menuItems
                        ?.map(String::lowercase)
                        ?.toSet()
                putStringSet(PREF_INTERFACE_MENU_ITEMS, menuItems)

                putBoolean(
                    PREF_INTERFACE_STUDIOS_AS_TEXT,
                    config.configuration.`interface`.showStudioAsText ?: false,
                )

                val defaultFilters = ui.getCaseInsensitive("defaultFilters")
                refreshDefaultFilters(defaultFilters as Map<String, *>?)
            }
        }
    }

    private fun refreshDefaultFilters(defaultFilters: Map<String, *>?) {
        val filterParser = FilterParser(serverVersion)

        DataType.entries.forEach { dataType ->
            val filterMap =
                defaultFilters?.getCaseInsensitive(dataType.filterMode.name) as Map<String, *>?
            val filter =
                if (filterMap != null) {
                    try {
                        filterParser.convertFilterMap(dataType, filterMap)
                    } catch (ex: Exception) {
                        Log.w(TAG, "default filter parse error for $dataType", ex)
                        FilterArgs(dataType)
                    }
                } else {
                    FilterArgs(dataType)
                }
            _defaultFilters[dataType] = filter
        }

        PageFilterKey.entries.forEach { key ->
            val filterMap =
                defaultFilters?.getCaseInsensitive(key.prefKey) as Map<String, *>?
            defaultPageFilters[key] =
                if (filterMap != null) {
                    try {
                        filterParser.convertFilterMap(key.dataType, filterMap, false)
                    } catch (ex: Exception) {
                        Log.w(TAG, "default filter parse error for $key", ex)
                        FilterArgs(key.dataType)
                    }
                } else {
                    FilterArgs(key.dataType)
                }
        }
    }

    fun getDefaultFilter(page: PageFilterKey): FilterArgs = defaultPageFilters[page] ?: FilterArgs(page.dataType)

    private fun parseScan(
        defaultScan: ConfigurationQuery.Scan?,
        uiTaskDefaults: Map<String, *>?,
    ): SharedPreferences.Editor.() -> Unit =
        {
            val scan = uiTaskDefaults?.getCaseInsensitive("scan") as Map<String, *>?
            putBoolean(
                PREF_SCAN_GENERATE_COVERS,
                scan.getBoolean("scanGenerateCovers", defaultScan?.scanGenerateCovers, false),
            )
            putBoolean(
                PREF_SCAN_GENERATE_PREVIEWS,
                scan.getBoolean("scanGeneratePreviews", defaultScan?.scanGeneratePreviews, false),
            )
            putBoolean(
                PREF_SCAN_GENERATE_IMAGE_PREVIEWS,
                scan.getBoolean(
                    "scanGenerateImagePreviews",
                    defaultScan?.scanGenerateImagePreviews,
                    false,
                ),
            )
            putBoolean(
                PREF_SCAN_GENERATE_SPRITES,
                scan.getBoolean("scanGenerateSprites", defaultScan?.scanGenerateSprites, false),
            )
            putBoolean(
                PREF_SCAN_GENERATE_PHASHES,
                scan.getBoolean("scanGeneratePhashes", defaultScan?.scanGeneratePhashes, false),
            )
            putBoolean(
                PREF_SCAN_GENERATE_THUMBNAILS,
                scan.getBoolean(
                    "scanGenerateThumbnails",
                    defaultScan?.scanGenerateThumbnails,
                    false,
                ),
            )
            putBoolean(
                PREF_SCAN_GENERATE_CLIP_PREVIEWS,
                scan.getBoolean(
                    "scanGenerateClipPreviews",
                    defaultScan?.scanGenerateClipPreviews,
                    false,
                ),
            )
        }

    private fun parseGenerate(
        defaultGenerate: ConfigurationQuery.Generate?,
        uiTaskDefaults: Map<String, *>?,
    ): SharedPreferences.Editor.() -> Unit =
        {
            val generate = uiTaskDefaults?.getCaseInsensitive("generate") as Map<String, *>?
            putBoolean(
                PREF_GEN_CLIP_PREVIEWS,
                generate.getBoolean("clipPreviews", defaultGenerate?.clipPreviews, false),
            )
            putBoolean(
                PREF_GEN_COVERS,
                generate.getBoolean("covers", defaultGenerate?.covers, false),
            )
            putBoolean(
                PREF_GEN_IMAGE_PREVIEWS,
                generate.getBoolean("imagePreviews", defaultGenerate?.imagePreviews, false),
            )
            putBoolean(
                PREF_GEN_INTERACTIVE_HEATMAPS_SPEEDS,
                generate.getBoolean(
                    "interactiveHeatmapsSpeeds",
                    defaultGenerate?.interactiveHeatmapsSpeeds,
                    false,
                ),
            )
            putBoolean(
                PREF_GEN_MARKER_IMAGE_PREVIEWS,
                generate.getBoolean(
                    "markerImagePreviews",
                    defaultGenerate?.markerImagePreviews,
                    false,
                ),
            )
            putBoolean(
                PREF_GEN_MARKERS,
                generate.getBoolean("markers", defaultGenerate?.markers, false),
            )
            putBoolean(
                PREF_GEN_MARKER_SCREENSHOTS,
                generate.getBoolean("markerScreenshots", defaultGenerate?.markerScreenshots, false),
            )
            putBoolean(
                PREF_GEN_PHASHES,
                generate.getBoolean("phashes", defaultGenerate?.phashes, false),
            )
            putBoolean(
                PREF_GEN_PREVIEWS,
                generate.getBoolean("previews", defaultGenerate?.previews, false),
            )
            putBoolean(
                PREF_GEN_SPRITES,
                generate.getBoolean("sprites", defaultGenerate?.sprites, false),
            )
            putBoolean(
                PREF_GEN_TRANSCODES,
                generate.getBoolean("transcodes", defaultGenerate?.transcodes, false),
            )
        }

    private fun Map<String, *>?.getBoolean(
        key: String,
        defaultsValue: Boolean?,
        default: Boolean,
    ): Boolean = this?.getCaseInsensitive(key)?.toString()?.toBoolean() ?: defaultsValue ?: default

    var scanJobId: String?
        get() = preferences.getString(PREF_JOB_SCAN, null)
        set(value) = preferences.edit { putString(PREF_JOB_SCAN, value) }

    var generateJobId: String?
        get() = preferences.getString(PREF_JOB_GENERATE, null)
        set(value) = preferences.edit { putString(PREF_JOB_GENERATE, value) }

    companion object {
        const val TAG = "ServerPreferences"
        val DEFAULT_MENU_ITEMS =
            setOf(
                "scenes",
                "images",
                "groups",
                "markers",
                "galleries",
                "performers",
                "studios",
                "tags",
            )

        const val PREF_SERVER_VERSION = "serverInfo.version"
        const val PREF_COMPANION_PLUGIN_VERSION = "companionPlugin.version"

        const val PREF_TRACK_ACTIVITY = "trackActivity"
        const val PREF_MINIMUM_PLAY_PERCENT = "minimumPlayPercent"
        const val PREF_RATING_TYPE = "ratingSystemOptions.type"
        const val PREF_RATING_PRECISION = "ratingSystemOptions.starPrecision"
        const val PREF_ALWAYS_START_BEGINNING = "ui.alwaysStartFromBeginning"

        // Scan default settings
        const val PREF_SCAN_GENERATE_COVERS = "scanGenerateCovers"
        const val PREF_SCAN_GENERATE_PREVIEWS = "scanGeneratePreviews"
        const val PREF_SCAN_GENERATE_IMAGE_PREVIEWS = "scanGenerateImagePreviews"
        const val PREF_SCAN_GENERATE_SPRITES = "scanGenerateSprites"
        const val PREF_SCAN_GENERATE_PHASHES = "scanGeneratePhashes"
        const val PREF_SCAN_GENERATE_THUMBNAILS = "scanGenerateThumbnails"
        const val PREF_SCAN_GENERATE_CLIP_PREVIEWS = "scanGenerateClipPreviews"

        // Generate default settings
        const val PREF_GEN_CLIP_PREVIEWS = "generate.clipPreviews"
        const val PREF_GEN_COVERS = "generate.covers"
        const val PREF_GEN_IMAGE_PREVIEWS = "generate.imagePreviews"
        const val PREF_GEN_INTERACTIVE_HEATMAPS_SPEEDS = "generate.interactiveHeatmapsSpeeds"
        const val PREF_GEN_MARKER_IMAGE_PREVIEWS = "generate.markerImagePreviews"
        const val PREF_GEN_MARKERS = "generate.markers"
        const val PREF_GEN_MARKER_SCREENSHOTS = "generate.markerScreenshots"
        const val PREF_GEN_PHASHES = "generate.phashes"
        const val PREF_GEN_PREVIEWS = "generate.previews"
        const val PREF_GEN_SPRITES = "generate.sprites"
        const val PREF_GEN_TRANSCODES = "generate.transcodes"
        const val PREF_GEN_IMAGE_THUMBNAILS = "generate.imageThumbnails"

        const val PREF_INTERFACE_MENU_ITEMS = "interface.menuItems"
        const val PREF_INTERFACE_STUDIOS_AS_TEXT = "interface.showStudioAsText"
        const val PREF_INTERFACE_ABBREV_COUNTERS = "interface.abbreviateCounters"

        const val PREF_JOB_SCAN = "app.job.scan"
        const val PREF_JOB_GENERATE = "app.job.generate"
    }
}
