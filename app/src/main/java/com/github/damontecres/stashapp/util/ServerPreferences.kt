package com.github.damontecres.stashapp.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.github.damontecres.stashapp.api.ConfigurationQuery

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

    val trackActivity get() = preferences.getBoolean(PREF_TRACK_ACTIVITY, false)

    val minimumPlayPercent get() = preferences.getInt(PREF_MINIMUM_PLAY_PERCENT, 20)

    suspend fun updatePreferences() {
        val queryEngine = QueryEngine(context)
        val query = ConfigurationQuery()
        val config = queryEngine.executeQuery(query).data?.configuration
        updatePreferences(config)
    }

    /**
     * Update the local preferences from the server configuration
     */
    fun updatePreferences(config: ConfigurationQuery.Configuration?) {
        if (config != null) {
            val ui = config.ui as Map<String, *>
            preferences.edit(true) {
                ui.getCaseInsensitive(PREF_TRACK_ACTIVITY).also {
                    putBoolean(PREF_TRACK_ACTIVITY, it.toString().toBoolean())
                }
                ui.getCaseInsensitive(PREF_MINIMUM_PLAY_PERCENT)?.let {
                    try {
                        putInt(PREF_MINIMUM_PLAY_PERCENT, it.toString().toInt())
                    } catch (ex: NumberFormatException) {
                        Log.w(TAG, "$PREF_MINIMUM_PLAY_PERCENT is not an integer: '$it'")
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
                    putBoolean(PREF_GEN_MARKERS, generate.markers ?: false)
                    putBoolean(PREF_GEN_MARKER_SCREENSHOTS, generate.markerScreenshots ?: false)
                    putBoolean(PREF_GEN_PHASHES, generate.phashes ?: false)
                    putBoolean(PREF_GEN_PREVIEWS, generate.previews ?: false)
                    putBoolean(PREF_GEN_SPRITES, generate.sprites ?: false)
                    putBoolean(PREF_GEN_TRANSCODES, generate.transcodes ?: false)
                }
            }
        }
    }

    companion object {
        const val TAG = "ServerPreferences"

        const val PREF_TRACK_ACTIVITY = "trackActivity"
        const val PREF_MINIMUM_PLAY_PERCENT = "minimumPlayPercent"

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
    }
}
