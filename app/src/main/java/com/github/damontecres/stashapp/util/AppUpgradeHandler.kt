package com.github.damontecres.stashapp.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.playback.resolutionFromLabel
import com.github.damontecres.stashapp.playback.streamChoiceFromLabel
import com.github.damontecres.stashapp.proto.AdvancedPreferences
import com.github.damontecres.stashapp.proto.CachePreferences
import com.github.damontecres.stashapp.proto.InterfacePreferences
import com.github.damontecres.stashapp.proto.PinPreferences
import com.github.damontecres.stashapp.proto.PlaybackFinishBehavior
import com.github.damontecres.stashapp.proto.PlaybackHttpClient
import com.github.damontecres.stashapp.proto.PlaybackPreferences
import com.github.damontecres.stashapp.proto.SearchPreferences
import com.github.damontecres.stashapp.proto.TabPreferences
import com.github.damontecres.stashapp.proto.ThemeStyle
import com.github.damontecres.stashapp.proto.UpdatePreferences
import com.github.damontecres.stashapp.ui.components.prefs.StashPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class AppUpgradeHandler(
    private val context: Context,
    private val previousVersion: Version,
    private val installedVersion: Version,
) : Runnable {
    companion object {
        private const val TAG = "AppUpgradeHandler"
    }

    override fun run() {
        UpdateChecker.cleanup(context)

        Log.i(TAG, "Migrating $previousVersion to $installedVersion")
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)

        // Add mpegts as a default force direct play format
        if (previousVersion.isEqualOrBefore(Version.fromString("0.2.9")) &&
            installedVersion.isAtLeast(Version.fromString("0.2.7"))
        ) {
            Log.d(TAG, "Checking for mpegts direct play")
            val defaultFormats =
                context.resources.getStringArray(R.array.default_force_container_formats).toSet()
            val current =
                preferences.getStringSet(
                    context.getString(R.string.pref_key_default_forced_direct_containers),
                    defaultFormats,
                )!!
            if (!current.contains("mpegts")) {
                preferences.edit {
                    val newSet = current.toMutableSet()
                    newSet.add("mpegts")
                    putStringSet(
                        context.getString(R.string.pref_key_default_forced_direct_containers),
                        newSet,
                    )
                }
            }
        }

        if (previousVersion.isEqualOrBefore(Version.fromString("v0.4.1"))) {
            val serverPreferences: SharedPreferences =
                context.getSharedPreferences(
                    context.packageName + "_server_preferences",
                    Context.MODE_PRIVATE,
                )
            serverPreferences.edit(true) {
                clear()
            }
        }

        if (previousVersion.isEqualOrBefore(Version.fromString("v0.5.2"))) {
            Log.i(TAG, "Migrating tabs for v0.5.2")
            preferences.ensureSetHas(
                context,
                R.string.pref_key_ui_tag_tabs,
                R.array.tag_tabs,
                listOf(
                    context.getString(R.string.stashapp_details),
                    context.getString(R.string.stashapp_parent_tags),
                ),
            )

            preferences.ensureSetHas(
                context,
                R.string.pref_key_ui_studio_tabs,
                R.array.studio_tabs,
                listOf(
                    context.getString(R.string.stashapp_details),
                ),
            )
        }
        if (previousVersion == Version.fromString("v0.5.2-8-gc2c5e6f")) {
            val key = context.getString(R.string.pref_key_read_only_mode_pin)
            val readOnlyPin = preferences.getInt(key, -1)
            if (readOnlyPin >= 0) {
                preferences.edit(true) {
                    remove(key)
                    putString(key, readOnlyPin.toString())
                }
            }
        }

        if (previousVersion.isEqualOrBefore(Version.fromString("v0.5.11-3-gf0cf79e2"))) {
            Log.i(TAG, "Migrating tabs for v0.5.11-3-gf0cf79e2")
            preferences.ensureSetHas(
                context,
                R.string.pref_key_ui_performer_tabs,
                R.array.performer_tabs,
                listOf(
                    context.getString(R.string.stashapp_studio),
                ),
            )
        }

        if (previousVersion.isLessThan(Version.fromString("0.6.6"))) {
            Log.i(TAG, "Setting new UI to true")
            val key = context.getString(R.string.pref_key_use_compose_ui)
            if (!preferences.getBoolean(key, true)) {
                // User turned on new UI and turned it off, so show a Toast
                Toast
                    .makeText(
                        context,
                        "The new UI is now the default. You can still switch back to the legacy UI in settings.",
                        Toast.LENGTH_LONG,
                    ).show()
            }
            preferences.edit(true) {
                putBoolean(key, true)
            }
        }
        if (previousVersion.isLessThan(Version.fromString("0.6.10"))) {
            try {
                preferences.getString(context.getString(R.string.pref_key_card_size), "5")
            } catch (_: ClassCastException) {
                val value = preferences.getInt(context.getString(R.string.pref_key_card_size), 5)
                preferences.edit(true) {
                    putString(context.getString(R.string.pref_key_card_size), value.toString())
                }
            }
        }

        if (previousVersion.isLessThan(Version.fromString("0.6.11"))) {
            CoroutineScope(Dispatchers.IO + StashCoroutineExceptionHandler()).launch {
                val preferencesMigratedV1 =
                    context.preferences.data
                        .map { it.preferencesMigratedV1 }
                        .first()
                if (!preferencesMigratedV1) {
                    migratePreferences()
                }
            }
        }
    }

    private fun SharedPreferences.ensureSetHas(
        context: Context,
        @StringRes prefKey: Int,
        @ArrayRes defaultValues: Int,
        newValues: Collection<String>,
    ) {
        val key = context.getString(prefKey)
        val currentValues =
            getStringSet(
                key,
                context.resources.getStringArray(defaultValues).toSet(),
            )!!.toMutableSet()
        if (currentValues.addAll(newValues)) {
            edit(true) {
                putStringSet(key, currentValues)
            }
        }
    }

    private suspend fun migratePreferences() {
        Log.i(TAG, "Starting preferences migration")
        val pm = PreferenceManager.getDefaultSharedPreferences(context)
        val s: (Int) -> String = context::getString
        val bool: (Int, Boolean) -> Boolean = { key, default ->
            pm.getBoolean(s(key), default)
        }
        val int: (Int, Int) -> Int = { key, default ->
            pm.getInt(s(key), default)
        }
        val string: (Int, String) -> String = { key, default ->
            pm.getString(s(key), default) ?: default
        }

        context.preferences.updateData {
            it
                .toBuilder()
                .apply {
                    preferencesMigratedV1 = true
                    pinPreferences =
                        PinPreferences
                            .newBuilder()
                            .apply {
                                pin = string(R.string.pref_key_pin_code, "")
                                readOnlyPin = string(R.string.pref_key_read_only_mode_pin, "")
                                autoSubmit = bool(R.string.pref_key_pin_code_auto, true)
                            }.build()
                    interfacePreferences =
                        InterfacePreferences
                            .newBuilder()
                            .apply {
                                useComposeUi = bool(R.string.pref_key_use_compose_ui, true)
                                cardSize =
                                    string(R.string.pref_key_card_size, "5").toIntOrNull() ?: 5
                                playVideoPreviews = pm.getBoolean("playVideoPreviews", true)
                                rememberSelectedTab = bool(R.string.pref_key_ui_remember_tab, true)
                                cardPreviewDelayMs =
                                    int(
                                        R.string.pref_key_ui_card_overlay_delay,
                                        StashPreference.VideoPreviewDelay.defaultValue,
                                    ).toLong()
                                slideShowIntervalMs =
                                    int(
                                        R.string.pref_key_slideshow_duration,
                                        5,
                                    ).seconds.inWholeMilliseconds
                                slideShowImageClipPauseMs =
                                    int(
                                        R.string.pref_key_slideshow_duration_image_clip,
                                        StashPreference.SlideshowImageClipDelay.defaultValue,
                                    ).toLong()
                                showGridJumpButtons =
                                    bool(R.string.pref_key_ui_grid_jump_controls, true)
                                theme = string(R.string.pref_key_ui_theme_file, "default")
                                themeStyle =
                                    string(
                                        R.string.pref_key_ui_theme_dark_appearance,
                                        "system",
                                    ).let {
                                        when (it.lowercase()) {
                                            "dark" -> ThemeStyle.THEME_STYLE_DARK
                                            "light" -> ThemeStyle.THEME_STYLE_LIGHT
                                            else -> ThemeStyle.THEME_STYLE_SYSTEM
                                        }
                                    }
                                showProgressWhenSkipping =
                                    bool(R.string.pref_key_playback_show_skip_progress, true)
                                playMovementSounds = bool(R.string.pref_key_movement_sounds, true)
                                useUpDownPreviousNext =
                                    bool(R.string.pref_key_playback_next_up_down, false)
                                captionsByDefault =
                                    bool(R.string.pref_key_captions_on_by_default, true)
                                scrollNextViewAll = pm.getBoolean("scrollToNextResult", true)
                                scrollTopOnBack = bool(R.string.pref_key_back_button_scroll, true)
                                showPositionFooter = bool(R.string.pref_key_show_grid_footer, true)
                                showRatingOnCards = bool(R.string.pref_key_show_rating, true)
                                videoPreviewAudio =
                                    bool(R.string.pref_key_video_preview_audio, false)
                                pageWithRemoteButtons =
                                    bool(R.string.pref_key_remote_page_buttons, true)
                                dpadSkipIndicator = bool(R.string.pref_key_show_dpad_skip, true)

                                tabPreferences =
                                    TabPreferences
                                        .newBuilder()
                                        .apply {
                                            addAllGallery(StashPreference.GalleryTab.defaultValue)
                                            addAllGroup(StashPreference.GroupTab.defaultValue)
                                            addAllPerformer(StashPreference.PerformerTab.defaultValue)
                                            addAllStudio(StashPreference.StudioTab.defaultValue)
                                            addAllTags(StashPreference.TagTab.defaultValue)
                                        }.build()
                            }.build()
                    playbackPreferences =
                        PlaybackPreferences
                            .newBuilder()
                            .apply {
                                skipForwardMs =
                                    pm.getInt("skip_forward_time", 30).seconds.inWholeMilliseconds
                                skipBackwardMs =
                                    pm.getInt("skip_back_time", 10).seconds.inWholeMilliseconds
                                playbackFinishBehavior =
                                    (
                                        pm.getString("playbackFinishedBehavior", null)
                                            ?: "Do nothing"
                                    ).let {
                                        when (it) {
                                            "Do nothing" -> PlaybackFinishBehavior.PLAYBACK_FINISH_BEHAVIOR_DO_NOTHING
                                            "Repeat scene" -> PlaybackFinishBehavior.PLAYBACK_FINISH_BEHAVIOR_REPEAT
                                            "Return to scene details" -> PlaybackFinishBehavior.PLAYBACK_FINISH_BEHAVIOR_GO_BACK
                                            else -> PlaybackFinishBehavior.PLAYBACK_FINISH_BEHAVIOR_DO_NOTHING
                                        }
                                    }
                                streamChoice =
                                    streamChoiceFromLabel(
                                        pm.getString("stream_choice", null) ?: "HLS",
                                    )
                                showDebugInfo =
                                    bool(R.string.pref_key_show_playback_debug_info, false)
                                dpadSkipping =
                                    bool(R.string.pref_key_playback_skip_left_right, true)
                                controllerTimeoutMs = pm.getInt("controllerShowTimeoutMs", 3_500)
                                savePlayHistory =
                                    bool(R.string.pref_key_playback_track_activity, true)
                                startWithNoAudio =
                                    bool(R.string.pref_key_playback_start_muted, false)
                                transcodeAboveResolution =
                                    resolutionFromLabel(
                                        string(
                                            R.string.pref_key_playback_always_transcode,
                                            "",
                                        ),
                                    )
                                enableVideoFilters = bool(R.string.pref_key_video_filters, false)
                                saveVideoFilters =
                                    bool(R.string.pref_key_playback_save_effects, true)
                                enableDebugLogging =
                                    bool(R.string.pref_key_playback_debug_logging, false)
                                playbackHttpClient =
                                    string(R.string.pref_key_playback_http_client, "okhttp").let {
                                        when (it) {
                                            "okhttp" -> PlaybackHttpClient.PLAYBACK_HTTP_CLIENT_OKHTTP
                                            "default" -> PlaybackHttpClient.PLAYBACK_HTTP_CLIENT_BUILTIN
                                            else -> PlaybackHttpClient.PLAYBACK_HTTP_CLIENT_OKHTTP
                                        }
                                    }

                                clearDirectPlayVideo()
                                addAllDirectPlayVideo(
                                    pm.getStringSet(
                                        s(R.string.pref_key_default_forced_direct_video),
                                        null,
                                    ) ?: StashPreference.DirectPlayVideo.defaultValue.toSet(),
                                )
                                clearDirectPlayAudio()
                                addAllDirectPlayAudio(
                                    pm.getStringSet(
                                        s(R.string.pref_key_default_forced_direct_audio),
                                        null,
                                    ) ?: StashPreference.DirectPlayAudio.defaultValue.toSet(),
                                )
                                clearDirectPlayFormat()
                                addAllDirectPlayAudio(
                                    pm.getStringSet(
                                        s(R.string.pref_key_default_forced_direct_containers),
                                        null,
                                    ) ?: StashPreference.DirectPlayFormat.defaultValue.toSet(),
                                )
                            }.build()
                    updatePreferences =
                        UpdatePreferences
                            .newBuilder()
                            .apply {
                                checkForUpdates = pm.getBoolean("autoCheckForUpdates", true)
                                updateUrl =
                                    pm.getStringNotNull(
                                        "updateCheckUrl",
                                        context.getString(R.string.app_update_url),
                                    )
                            }.build()
                    advancedPreferences =
                        AdvancedPreferences
                            .newBuilder()
                            .apply {
                                logErrorsToServer = bool(R.string.pref_key_log_to_server, true)
                                networkTimeoutMs =
                                    pm
                                        .getInt(
                                            "networkTimeout",
                                            15,
                                        ).seconds.inWholeMilliseconds
                                trustSelfSignedCertificates =
                                    bool(R.string.pref_key_trust_certs, false)
                                imageThreadCount =
                                    int(
                                        R.string.pref_key_image_loading_threads,
                                        Runtime.getRuntime().availableProcessors(),
                                    )
                            }.build()
                    cachePreferences =
                        CachePreferences
                            .newBuilder()
                            .apply {
                                networkCacheSize =
                                    int(R.string.pref_key_network_cache_size, 10)
                                        .toLong() * 1024 * 1024
                                imageDiskCacheSize =
                                    int(R.string.pref_key_image_cache_size, 10)
                                        .toLong() * 1024 * 1024
                                cacheExpirationTime = pm.getInt("networkCacheDuration", 6)
                                logCacheHits = pm.getBoolean("networkCacheLogging", false)
                            }.build()
                    searchPreferences =
                        SearchPreferences
                            .newBuilder()
                            .apply {
                                maxResults = int(R.string.pref_key_max_search_results, 25)
                                searchDelayMs = int(R.string.pref_key_search_delay, 1000).toLong()
                            }.build()
                }.build()
        }
        Log.i(TAG, "Finished preferences migration")
    }
}
