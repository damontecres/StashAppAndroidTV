package com.github.damontecres.stashapp.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.github.damontecres.stashapp.api.ConfigurationQuery
import com.github.damontecres.stashapp.api.ServerInfoQuery

/**
 * Represents configuration that users have set server-side
 *
 * Configuration is loaded into a SharedPreferences and made available throughout the app
 */
class ServerPreferences(private val context: Context) {
    val preferences: SharedPreferences =
        context.getSharedPreferences(
            context.packageName + "_server_preferences",
            Context.MODE_PRIVATE,
        )

    val serverVersion
        get() =
            Version.fromString(
                preferences.getStringNotNull(
                    PREF_SERVER_VERSION,
                    "0.0.0",
                ),
            )

    val trackActivity get() = preferences.getBoolean(PREF_TRACK_ACTIVITY, false)

    val minimumPlayPercent get() = preferences.getInt(PREF_MINIMUM_PLAY_PERCENT, 20)

    val ratingsAsStars get() = preferences.getString(PREF_RATING_TYPE, "stars") == "stars"

    suspend fun updatePreferences(): ServerPreferences {
        val queryEngine = QueryEngine(context)
        val query = ConfigurationQuery()
        val config = queryEngine.executeQuery(query).data?.configuration
        val serverInfo = queryEngine.executeQuery(ServerInfoQuery()).data
        updatePreferences(config, serverInfo)
        return this
    }

    /**
     * Update the local preferences from the server configuration
     */
    private fun updatePreferences(
        config: ConfigurationQuery.Configuration?,
        serverInfo: ServerInfoQuery.Data?,
    ) {
        if (config != null) {
            val ui = config.ui as Map<String, *>
            preferences.edit(true) {
                ui.getCaseInsensitive(PREF_TRACK_ACTIVITY).also {
                    // If null, toString()=>"null".toBoolean()=>false
                    putBoolean(PREF_TRACK_ACTIVITY, it.toString().toBoolean())
                }
                ui.getCaseInsensitive(PREF_MINIMUM_PLAY_PERCENT)?.let {
                    try {
                        putInt(PREF_MINIMUM_PLAY_PERCENT, it.toString().toInt())
                    } catch (ex: NumberFormatException) {
                        Log.w(TAG, "$PREF_MINIMUM_PLAY_PERCENT is not an integer: '$it'")
                    }
                }
                val ratingSystemOptionsRaw = ui.getCaseInsensitive("ratingSystemOptions")
                if (ratingSystemOptionsRaw != null) {
                    try {
                        val ratingSystemOptions = ratingSystemOptionsRaw as Map<String, String>
                        val type = ratingSystemOptions["type"] ?: "star"
                        val starPrecision =
                            when (ratingSystemOptions["starPrecision"]?.lowercase()) {
                                "full" -> 1.0f
                                "half" -> 0.5f
                                "quarter" -> 0.25f
                                "tenth" -> 0.1f
                                else -> null
                            }
                        putString(PREF_RATING_TYPE, type)
                        if (starPrecision != null) {
                            putFloat(PREF_RATING_PRECISION, starPrecision)
                        }
                    } catch (ex: Exception) {
                        Log.e(
                            TAG,
                            "Exception parsing ratingSystemOptions: $ratingSystemOptionsRaw",
                        )
                    }
                }

                val scan = config.defaults.scan
                if (scan != null) {
                    putBoolean(PREF_SCAN_GENERATE_COVERS, scan.scanGenerateCovers)
                    putBoolean(PREF_SCAN_GENERATE_PREVIEWS, scan.scanGeneratePreviews)
                    putBoolean(PREF_SCAN_GENERATE_IMAGE_PREVIEWS, scan.scanGenerateImagePreviews)
                    putBoolean(PREF_SCAN_GENERATE_SPRITES, scan.scanGenerateSprites)
                    putBoolean(PREF_SCAN_GENERATE_PHASHES, scan.scanGeneratePhashes)
                    putBoolean(PREF_SCAN_GENERATE_THUMBNAILS, scan.scanGenerateThumbnails)
                    putBoolean(PREF_SCAN_GENERATE_CLIP_PREVIEWS, scan.scanGenerateClipPreviews)
                }

                val generate = config.defaults.generate
                if (generate != null) {
                    putBoolean(PREF_GEN_CLIP_PREVIEWS, generate.clipPreviews ?: false)
                    putBoolean(PREF_GEN_COVERS, generate.covers ?: false)
                    putBoolean(PREF_GEN_IMAGE_PREVIEWS, generate.imagePreviews ?: false)
                    putBoolean(
                        PREF_GEN_INTERACTIVE_HEATMAPS_SPEEDS,
                        generate.interactiveHeatmapsSpeeds ?: false,
                    )
                    putBoolean(
                        PREF_GEN_MARKER_IMAGE_PREVIEWS,
                        generate.markerImagePreviews ?: false,
                    )
                    putBoolean(PREF_GEN_IMAGE_THUMBNAILS, generate.imageThumbnails ?: false)
                    putBoolean(PREF_GEN_MARKERS, generate.markers ?: false)
                    putBoolean(PREF_GEN_MARKER_SCREENSHOTS, generate.markerScreenshots ?: false)
                    putBoolean(PREF_GEN_PHASHES, generate.phashes ?: false)
                    putBoolean(PREF_GEN_PREVIEWS, generate.previews ?: false)
                    putBoolean(PREF_GEN_SPRITES, generate.sprites ?: false)
                    putBoolean(PREF_GEN_TRANSCODES, generate.transcodes ?: false)
                }

                val menuItems = config.`interface`.menuItems?.map(String::lowercase)?.toSet()
                putStringSet(PREF_INTERFACE_MENU_ITEMS, menuItems)
            }
        }

        if (serverInfo != null) {
            preferences.edit(true) {
                putString(PREF_SERVER_VERSION, serverInfo.version.version)
            }
        }
    }

    companion object {
        const val TAG = "ServerPreferences"
        val DEFAULT_MENU_ITEMS =
            setOf("scenes", "movies", "markers", "performers", "studios", "tags")

        const val PREF_SERVER_VERSION = "serverInfo.version"

        const val PREF_TRACK_ACTIVITY = "trackActivity"
        const val PREF_MINIMUM_PLAY_PERCENT = "minimumPlayPercent"
        const val PREF_RATING_TYPE = "ratingSystemOptions.type"
        const val PREF_RATING_PRECISION = "ratingSystemOptions.starPrecision"

        // Scan default settings
        const val PREF_SCAN_GENERATE_COVERS = "scanGenerateCovers"
        const val PREF_SCAN_GENERATE_PREVIEWS = "scanGeneratePreviews"
        const val PREF_SCAN_GENERATE_IMAGE_PREVIEWS = "scanGenerateImagePreviews"
        const val PREF_SCAN_GENERATE_SPRITES = "scanGenerateSprites"
        const val PREF_SCAN_GENERATE_PHASHES = "scanGeneratePhashes"
        const val PREF_SCAN_GENERATE_THUMBNAILS = "scanGenerateThumbnails"
        const val PREF_SCAN_GENERATE_CLIP_PREVIEWS = "scanGenerateClipPreviews"

        // Generate default settings
        const val PREF_GEN_CLIP_PREVIEWS = "clipPreviews"
        const val PREF_GEN_COVERS = "covers"
        const val PREF_GEN_IMAGE_PREVIEWS = "imagePreviews"
        const val PREF_GEN_INTERACTIVE_HEATMAPS_SPEEDS = "interactiveHeatmapsSpeeds"
        const val PREF_GEN_MARKER_IMAGE_PREVIEWS = "markerImagePreviews"
        const val PREF_GEN_MARKERS = "markers"
        const val PREF_GEN_MARKER_SCREENSHOTS = "markerScreenshots"
        const val PREF_GEN_PHASHES = "phashes"
        const val PREF_GEN_PREVIEWS = "previews"
        const val PREF_GEN_SPRITES = "sprites"
        const val PREF_GEN_TRANSCODES = "transcodes"
        const val PREF_GEN_IMAGE_THUMBNAILS = "imageThumbnails"

        const val PREF_INTERFACE_MENU_ITEMS = "interface.menuItems"
    }
}
