package com.github.damontecres.stashapp.ui.components.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.PreferenceScreenOption
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.proto.PlaybackFinishBehavior
import com.github.damontecres.stashapp.proto.PlaybackHttpClient
import com.github.damontecres.stashapp.proto.Resolution
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.proto.StreamChoice
import com.github.damontecres.stashapp.proto.TabType
import com.github.damontecres.stashapp.proto.ThemeStyle
import com.github.damontecres.stashapp.util.cacheDurationPrefToDuration
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.updateAdvancedPreferences
import com.github.damontecres.stashapp.util.updateCachePreferences
import com.github.damontecres.stashapp.util.updateInterfacePreferences
import com.github.damontecres.stashapp.util.updatePinPreferences
import com.github.damontecres.stashapp.util.updatePlaybackPreferences
import com.github.damontecres.stashapp.util.updateSearchPreferences
import com.github.damontecres.stashapp.util.updateTabPreferences
import com.github.damontecres.stashapp.util.updateUpdatePreferences
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * A preference that can be stored in the shared preferences.
 *
 * @param T The type of the preference value.
 */
sealed interface StashPreference<T> {
    @get:StringRes
    val title: Int

    @get:StringRes
    val prefKey: Int

    val defaultValue: T

    val getter: (prefs: StashPreferences) -> T
    val setter: (prefs: StashPreferences, value: T) -> StashPreferences

    val prefGetter: (context: Context, prefs: SharedPreferences) -> T
    val prefSetter: (context: Context, prefs: SharedPreferences.Editor, value: T) -> SharedPreferences.Editor

    fun summary(
        context: Context,
        value: T?,
    ): String? = null

    fun validate(value: T): PreferenceValidation = PreferenceValidation.Valid

    companion object {
        // Basic
        val CurrentServer =
            StashClickablePreference(
                title = R.string.current_server,
            )
        val ManageServers =
            StashDestinationPreference(
                title = R.string.manage_servers,
                summary = R.string.add_remove_servers_summary,
                destination = Destination.ManageServers(true),
            )
        val AutoSubmitPin =
            StashSwitchPreference(
                title = R.string.auto_submit_pin,
                prefKey = R.string.pref_key_pin_code_auto,
                summary = R.string.auto_submit_pin_summary,
                defaultValue = true,
                getter = { it.pinPreferences.autoSubmit },
                setter = { prefs, value ->
                    prefs.updatePinPreferences { autoSubmit = value }
                },
            )
        val PinCode =
            StashPinPreference(
                title = R.string.pin_code,
                prefKey = R.string.pref_key_pin_code,
                defaultValue = "",
                description = R.string.set_app_pin_code,
                getter = { it.pinPreferences.pin },
                setter = { prefs, value ->
                    prefs.updatePinPreferences { pin = value }
                },
            )
        val CardSize =
            StashChoicePreference<Int>(
                title = R.string.card_size_title,
                prefKey = R.string.pref_key_card_size,
                defaultValue = 5,
                displayValues = R.array.card_sizes,
                indexToValue = { listOf(7, 6, 5, 4, 3)[it] },
                valueToIndex = { listOf(7, 6, 5, 4, 3).indexOf(it) },
                getter = { it.interfacePreferences.cardSize },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences { cardSize = value }
                },
                prefGetter = { context: Context, prefs: SharedPreferences ->
                    val value =
                        prefs.getString(
                            context.getString(R.string.pref_key_card_size),
                            null,
                        )
                    value?.toIntOrNull() ?: 5
                },
                prefSetter = { context: Context, editor: SharedPreferences.Editor, value: Int ->
                    editor.putString(
                        context.getString(R.string.pref_key_card_size),
                        value.toString(),
                    )
                },
            )
        val PlayVideoPreviews =
            StashSwitchPreference(
                title = R.string.play_video_previews,
                prefKey = R.string.pref_key_card_previews,
                defaultValue = true,
                getter = { it.interfacePreferences.playVideoPreviews },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences { playVideoPreviews = value }
                },
                summaryOn = R.string.play_video_previews_summary_on,
                summaryOff = R.string.play_video_previews_summary_off,
            )
        val MoreUiSettings =
            StashDestinationPreference(
                title = R.string.more_ui_settings,
                summary = R.string.more_ui_settings_summary,
                destination = Destination.Settings(PreferenceScreenOption.USER_INTERFACE),
            )

        val SkipForward =
            StashSliderPreference(
                title = R.string.skip_forward_preference,
                prefKey = R.string.pref_key_skip_forward_time,
                defaultValue = 30,
                min = 10,
                max = 5.minutes.inWholeSeconds.toInt(),
                interval = 5,
                getter = {
                    it.playbackPreferences.skipForwardMs
                        .milliseconds.inWholeSeconds
                        .toInt()
                },
                setter = { prefs, value ->
                    prefs.updatePlaybackPreferences {
                        skipForwardMs = value.seconds.inWholeMilliseconds
                    }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
                summarizer = { value ->
                    if (value != null) {
                        "$value seconds"
                    } else {
                        null
                    }
                },
            )

        val SkipBack =
            StashSliderPreference(
                title = R.string.skip_back_preference,
                prefKey = R.string.pref_key_skip_back_time,
                defaultValue = 10,
                min = 5,
                max = 5.minutes.inWholeSeconds.toInt(),
                interval = 5,
                getter = {
                    it.playbackPreferences.skipBackwardMs
                        .milliseconds.inWholeSeconds
                        .toInt()
                },
                setter = { prefs, value ->
                    prefs.updatePlaybackPreferences {
                        skipBackwardMs = value.seconds.inWholeMilliseconds
                    }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
                summarizer = { value ->
                    if (value != null) {
                        "$value seconds"
                    } else {
                        null
                    }
                },
            )
        val FinishedBehavior =
            StashChoicePreference<PlaybackFinishBehavior>(
                title = R.string.playback_finished_behavior,
                prefKey = R.string.pref_key_playback_finished_behavior,
                defaultValue = PlaybackFinishBehavior.DO_NOTHING,
                displayValues = R.array.playback_finished_behavior_options,
                indexToValue = {
                    PlaybackFinishBehavior.forNumber(it)
                },
                valueToIndex = { it.number },
                getter = { it.playbackPreferences.playbackFinishBehavior },
                setter = { prefs, value ->
                    prefs.updatePlaybackPreferences {
                        playbackFinishBehavior = value
                    }
                },
                prefGetter = { context: Context, prefs: SharedPreferences ->
                    val value =
                        prefs.getString(
                            context.getString(R.string.pref_key_playback_finished_behavior),
                            null,
                        )
                    value?.let { PlaybackFinishBehavior.valueOf(it.uppercase()) }
                        ?: PlaybackFinishBehavior.DO_NOTHING
                },
                prefSetter = { context: Context, editor: SharedPreferences.Editor, value: PlaybackFinishBehavior ->
                    editor.putString(
                        context.getString(R.string.pref_key_playback_finished_behavior),
                        value.name.lowercase(),
                    )
                },
            )

        val ReadOnlyMode =
            StashPinPreference(
                title = R.string.read_only_mode,
                prefKey = R.string.pref_key_read_only_mode_pin,
                defaultValue = "",
                description = R.string.read_only_pin_description,
                getter = { it.pinPreferences.readOnlyPin },
                setter = { prefs, value ->
                    prefs.updatePinPreferences { readOnlyPin = value }
                },
            )
        val InstalledVersion =
            StashClickablePreference(
                title = R.string.stashapp_package_manager_installed_version,
                getter = { },
                setter = { prefs, _ -> prefs },
            )
        val Update =
            StashClickablePreference(
                title = R.string.stashapp_package_manager_check_for_updates,
                getter = { },
                setter = { prefs, _ -> prefs },
            )

        val SendLogs =
            StashClickablePreference(
                title = R.string.send_logs,
                summary = R.string.send_logs_summary,
                getter = { },
                setter = { prefs, _ -> prefs },
            )

        val AdvancedSettings =
            StashDestinationPreference(
                title = R.string.advanced_settings,
                summary = R.string.advanced_settings_summary,
                destination = Destination.Settings(PreferenceScreenOption.ADVANCED),
            )
        // End basic

        // More UI settings
        val RememberTab =
            StashSwitchPreference(
                title = R.string.remember_selected_tab,
                prefKey = R.string.pref_key_ui_remember_tab,
                defaultValue = true,
                getter = { it.interfacePreferences.rememberSelectedTab },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences { rememberSelectedTab = value }
                },
                summaryOn = R.string.remember_selected_tab_summary_on,
                summaryOff = R.string.remember_selected_tab_summary_off,
            )

        val VideoPreviewDelay =
            StashSliderPreference(
                title = R.string.video_preview_delay,
                prefKey = R.string.pref_key_ui_card_overlay_delay,
                defaultValue = 1_000,
                min = 0,
                max = 10_000,
                interval = 100,
                getter = { it.interfacePreferences.cardPreviewDelayMs.toInt() },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences {
                        cardPreviewDelayMs = value.toLong()
                    }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
                summarizer = { value -> value?.let { "${value / 1000.0} seconds" } },
            )

        val SlideshowDuration =
            StashSliderPreference(
                title = R.string.slideshow_duration,
                prefKey = R.string.pref_key_slideshow_duration,
                defaultValue = 5.seconds.inWholeMilliseconds.toInt(),
                min = 1.seconds.inWholeMilliseconds.toInt(),
                max = 60.seconds.inWholeMilliseconds.toInt(),
                interval = 1.seconds.inWholeMilliseconds.toInt(),
                getter = { it.interfacePreferences.slideShowIntervalMs.toInt() },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences {
                        slideShowIntervalMs = value.toLong()
                    }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
                summarizer = { value -> value?.let { "${value / 1000.0} seconds" } },
            )

        val SlideshowImageClipDelay =
            StashSliderPreference(
                title = R.string.slideshow_image_clip_delay,
                prefKey = R.string.pref_key_slideshow_duration_image_clip,
                defaultValue = 250,
                min = 0,
                max = 60.seconds.inWholeMilliseconds.toInt(),
                interval = 250,
                getter = { it.interfacePreferences.slideShowImageClipPauseMs.toInt() },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences {
                        slideShowImageClipPauseMs = value.toLong()
                    }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
                summarizer = { value -> value?.let { "${value / 1000.0} seconds" } },
            )

        val UseNewUI =
            StashSwitchPreference(
                title = R.string.use_new_ui,
                prefKey = R.string.pref_key_use_compose_ui,
                defaultValue = true,
                getter = { it.interfacePreferences.useComposeUi },
                setter = { prefs, value ->
                    val context = StashApplication.getApplication()
                    PreferenceManager
                        .getDefaultSharedPreferences(context)
                        .edit(true) {
                            putBoolean(context.getString(R.string.pref_key_use_compose_ui), value)
                        }
                    prefs.updateInterfacePreferences { useComposeUi = value }
                },
                summaryOn = R.string.stashapp_actions_enable,
                summaryOff = R.string.transcode_options_disabled,
            )

        val GridJumpButtons =
            StashSwitchPreference(
                title = R.string.show_grid_jump_buttons,
                prefKey = R.string.pref_key_ui_grid_jump_controls,
                defaultValue = true,
                getter = { it.interfacePreferences.showGridJumpButtons },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences { showGridJumpButtons = value }
                },
                summaryOn = R.string.stashapp_actions_enable,
                summaryOff = R.string.transcode_options_disabled,
            )

        val ChooseTheme =
            StashDestinationPreference(
                title = R.string.choose_theme,
                summary = null,
                destination = Destination.ChooseTheme,
            )

        val ThemeStylePref =
            StashChoicePreference<ThemeStyle>(
                title = R.string.theme_style_preference_title,
                prefKey = R.string.pref_key_ui_theme_dark_appearance,
                defaultValue = ThemeStyle.SYSTEM,
                displayValues = R.array.ui_theme_dark_appearance_choices,
                indexToValue = {
                    ThemeStyle.forNumber(it)
                },
                valueToIndex = { it.number },
                getter = { it.interfacePreferences.themeStyle },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences {
                        themeStyle = value
                    }
                },
                prefGetter = { context: Context, prefs: SharedPreferences ->
                    val value =
                        prefs.getString(
                            context.getString(R.string.pref_key_ui_theme_dark_appearance),
                            null,
                        )
                    value?.let { ThemeStyle.valueOf(it.uppercase()) }
                        ?: ThemeStyle.SYSTEM
                },
                prefSetter = { context: Context, editor: SharedPreferences.Editor, value: ThemeStyle ->
                    editor.putString(
                        context.getString(R.string.pref_key_ui_theme_dark_appearance),
                        value.name.lowercase(),
                    )
                },
            )

        val ShowProgressSkipping =
            StashSwitchPreference(
                title = R.string.show_progress_when_skipping,
                prefKey = R.string.pref_key_playback_show_skip_progress,
                defaultValue = true,
                getter = { it.interfacePreferences.showProgressWhenSkipping },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences { showProgressWhenSkipping = value }
                },
                summaryOn = R.string.stashapp_actions_enable,
                summaryOff = R.string.transcode_options_disabled,
            )

        val MovementSound =
            StashSwitchPreference(
                title = R.string.movement_sounds,
                prefKey = R.string.pref_key_movement_sounds,
                defaultValue = true,
                getter = { it.interfacePreferences.playMovementSounds },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences { playMovementSounds = value }
                },
                summary = R.string.movement_sounds_summary,
            )

        val UpDownNextPrevious =
            StashSwitchPreference(
                title = R.string.up_down_next_previous_pref_title,
                prefKey = R.string.pref_key_playback_next_up_down,
                defaultValue = false,
                getter = { it.interfacePreferences.useUpDownPreviousNext },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences { useUpDownPreviousNext = value }
                },
                summaryOn = R.string.enabled_for_markers,
                summaryOff = R.string.transcode_options_disabled,
            )

        val Captions =
            StashSwitchPreference(
                title = R.string.captions_default,
                prefKey = R.string.pref_key_captions_on_by_default,
                defaultValue = true,
                getter = { it.interfacePreferences.captionsByDefault },
                setter = { prefs, value ->
                    prefs.updateInterfacePreferences { captionsByDefault = value }
                },
                summary = R.string.captions_default_summary,
            )

        private val GalleryTabList =
            listOf(
                TabType.DETAILS,
                TabType.IMAGES,
                TabType.SCENES,
                TabType.PERFORMERS,
                TabType.TAGS,
            )
        val GalleryTab =
            StashMultiChoicePreference<TabType>(
                title = R.string.stashapp_gallery,
                prefKey = R.string.pref_key_ui_gallery_tabs,
                defaultValue = GalleryTabList,
                allValues = GalleryTabList,
                displayValues = R.array.gallery_tabs,
                getter = { it.interfacePreferences.tabPreferences.galleryList },
                setter = { prefs, value ->
                    prefs.updateTabPreferences {
                        clearGallery()
                        addAllGallery(value)
                    }
                },
                toSharedPrefs = ::toPrefString,
                fromSharedPrefs = ::fromPrefString,
            )

        private val GroupTabList =
            listOf(
                TabType.DETAILS,
                TabType.SCENES,
                TabType.MARKERS,
                TabType.TAGS,
                TabType.CONTAINING_GROUPS,
                TabType.SUB_GROUPS,
            )
        val GroupTab =
            StashMultiChoicePreference<TabType>(
                title = R.string.stashapp_group,
                prefKey = R.string.pref_key_ui_group_tabs,
                defaultValue = GroupTabList,
                allValues = GroupTabList,
                displayValues = R.array.group_tabs,
                getter = { it.interfacePreferences.tabPreferences.groupList },
                setter = { prefs, value ->
                    prefs.updateTabPreferences {
                        clearGroup()
                        addAllGroup(value)
                    }
                },
                toSharedPrefs = ::toPrefString,
                fromSharedPrefs = ::fromPrefString,
            )

        val PerformerTabList =
            listOf(
                TabType.DETAILS,
                TabType.SCENES,
                TabType.GALLERIES,
                TabType.IMAGES,
                TabType.GROUPS,
                TabType.TAGS,
                TabType.APPEARS_WITH,
                TabType.MARKERS,
                TabType.STUDIOS,
            )
        val PerformerTab =
            StashMultiChoicePreference<TabType>(
                title = R.string.stashapp_performer,
                prefKey = R.string.pref_key_ui_performer_tabs,
                defaultValue = PerformerTabList,
                allValues = PerformerTabList,
                displayValues = R.array.performer_tabs,
                getter = { it.interfacePreferences.tabPreferences.performerList },
                setter = { prefs, value ->
                    prefs.updateTabPreferences {
                        clearPerformer()
                        addAllPerformer(value)
                    }
                },
                toSharedPrefs = ::toPrefString,
                fromSharedPrefs = ::fromPrefString,
            )

        private val StudioTabList =
            listOf(
                TabType.DETAILS,
                TabType.SCENES,
                TabType.GALLERIES,
                TabType.IMAGES,
                TabType.PERFORMERS,
                TabType.GROUPS,
                TabType.TAGS,
                TabType.SUBSIDIARY_STUDIOS,
                TabType.MARKERS,
            )
        val StudioTab =
            StashMultiChoicePreference<TabType>(
                title = R.string.stashapp_studio,
                prefKey = R.string.pref_key_ui_studio_tabs,
                defaultValue = StudioTabList,
                allValues = StudioTabList,
                displayValues = R.array.studio_tabs,
                getter = { it.interfacePreferences.tabPreferences.studioList },
                setter = { prefs, value ->
                    prefs.updateTabPreferences {
                        clearStudio()
                        addAllStudio(value)
                    }
                },
                toSharedPrefs = ::toPrefString,
                fromSharedPrefs = ::fromPrefString,
            )

        private val TagTabList =
            listOf(
                TabType.DETAILS,
                TabType.SCENES,
                TabType.GALLERIES,
                TabType.IMAGES,
                TabType.MARKERS,
                TabType.PERFORMERS,
                TabType.STUDIOS,
                TabType.SUB_TAGS,
                TabType.PARENT_TAGS,
            )
        val TagTab =
            StashMultiChoicePreference<TabType>(
                title = R.string.stashapp_tag,
                prefKey = R.string.pref_key_ui_tag_tabs,
                defaultValue = TagTabList,
                allValues = TagTabList,
                displayValues = R.array.tag_tabs,
                getter = { it.interfacePreferences.tabPreferences.tagsList },
                setter = { prefs, value ->
                    prefs.updateTabPreferences {
                        clearTags()
                        addAllTags(value)
                    }
                },
                toSharedPrefs = ::toPrefString,
                fromSharedPrefs = ::fromPrefString,
            )

        val TabPrefs =
            listOf(
                GalleryTab,
                GroupTab,
                PerformerTab,
                StudioTab,
                TagTab,
            )

        // Advanced
        val advancedUiPrefs =
            listOf(
                StashSwitchPreference(
                    title = R.string.scroll_next_view_all,
                    prefKey = R.string.pref_key_scroll_on_view_all,
                    defaultValue = true,
                    getter = { it.interfacePreferences.scrollNextViewAll },
                    setter = { prefs, value ->
                        prefs.updateInterfacePreferences { scrollNextViewAll = value }
                    },
                    summary = R.string.scroll_next_view_all_summary,
                ),
                StashSwitchPreference(
                    title = R.string.scroll_to_top_back,
                    prefKey = R.string.scroll_to_top_back,
                    defaultValue = true,
                    getter = { it.interfacePreferences.scrollTopOnBack },
                    setter = { prefs, value ->
                        prefs.updateInterfacePreferences { scrollTopOnBack = value }
                    },
                    summary = R.string.scroll_to_top_back_summary,
                ),
                StashSwitchPreference(
                    title = R.string.grid_position_footer,
                    prefKey = R.string.pref_key_show_grid_footer,
                    defaultValue = true,
                    getter = { it.interfacePreferences.showPositionFooter },
                    setter = { prefs, value ->
                        prefs.updateInterfacePreferences { showPositionFooter = value }
                    },
                    summaryOn = R.string.stashapp_actions_show,
                    summaryOff = R.string.stashapp_actions_hide,
                ),
                StashSwitchPreference(
                    title = R.string.show_card_rating,
                    prefKey = R.string.pref_key_show_rating,
                    defaultValue = true,
                    getter = { it.interfacePreferences.showRatingOnCards },
                    setter = { prefs, value ->
                        prefs.updateInterfacePreferences { showRatingOnCards = value }
                    },
                    summaryOn = R.string.stashapp_actions_show,
                    summaryOff = R.string.stashapp_actions_hide,
                ),
                StashSwitchPreference(
                    title = R.string.play_preview_audio,
                    prefKey = R.string.pref_key_video_preview_audio,
                    defaultValue = false,
                    getter = { it.interfacePreferences.videoPreviewAudio },
                    setter = { prefs, value ->
                        prefs.updateInterfacePreferences { videoPreviewAudio = value }
                    },
                    summaryOn = R.string.stashapp_actions_enable,
                    summaryOff = R.string.transcode_options_disabled,
                ),
                StashSwitchPreference(
                    title = R.string.page_with_remote_buttons,
                    prefKey = R.string.pref_key_remote_page_buttons,
                    defaultValue = true,
                    getter = { it.interfacePreferences.pageWithRemoteButtons },
                    setter = { prefs, value ->
                        prefs.updateInterfacePreferences { pageWithRemoteButtons = value }
                    },
                    summaryOn = R.string.page_with_remote_buttons_summary_on,
                    summaryOff = R.string.transcode_options_disabled,
                ),
            )

        val DirectPlayVideo =
            StashMultiChoicePreference<String>(
                title = R.string.direct_play_video,
                prefKey = R.string.pref_key_default_forced_direct_video,
                summary = R.string.direct_play_video_summary,
                defaultValue = listOf(),
                allValues =
                    listOf(
                        "av1",
                        "h263",
                        "h264",
                        "hevc",
                        "mpeg2video",
                        "mpeg4",
                        "vc1",
                        "vp8",
                        "vp9",
                        // Less common codecs
                        "flv1",
                        "mpeg1video",
                        "rv30",
                        "rv40",
                        "svq3",
                        "vc1",
                        "vp6f",
                        "wmv1",
                        "wmv2",
                        "wmv3",
                    ),
                displayValues = R.array.video_codecs,
                getter = { it.playbackPreferences.directPlayVideoList },
                setter = { prefs, value ->
                    prefs.updatePlaybackPreferences {
                        clearDirectPlayVideo()
                        addAllDirectPlayVideo(value)
                    }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
            )

        val DirectPlayAudio =
            StashMultiChoicePreference<String>(
                title = R.string.direct_play_audio,
                prefKey = R.string.pref_key_default_forced_direct_audio,
                summary = R.string.direct_play_audio_summary,
                defaultValue =
                    listOf(
                        "ac3",
                        "aac",
                        "dts",
                        "truehd",
                        "pcm_alaw",
                        "pcm_f32le",
                        "pcm_mulaw",
                        "pcm_s16be",
                        "pcm_s16le",
                        "pcm_s24le",
                        "pcm_s32le",
                        "pcm_s8",
                    ),
                allValues =
                    listOf(
                        "aac",
                        "ac3",
                        "ac4",
                        "dts",
                        "eac3",
                        "flac",
                        "mp3",
                        "opus",
                        "truehd",
                        "vorbis",
                        // PCM formats
                        "pcm_s8",
                        "pcm_s16be",
                        "pcm_s16le",
                        "pcm_s24le",
                        "pcm_s32le",
                        "pcm_f32le",
                        "pcm_alaw",
                        "pcm_mulaw",
                        // Less common codecs
                        "nellymoser",
                        "sipr",
                        "wmav2",
                        "wmapro",
                    ),
                displayValues = R.array.audio_codecs,
                getter = { it.playbackPreferences.directPlayAudioList },
                setter = { prefs, value ->
                    prefs.updatePlaybackPreferences {
                        clearDirectPlayAudio()
                        addAllDirectPlayAudio(value)
                    }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
            )

        val DirectPlayFormat =
            StashMultiChoicePreference<String>(
                title = R.string.direct_play_format,
                prefKey = R.string.pref_key_default_forced_direct_containers,
                summary = R.string.direct_play_format_summary,
                defaultValue =
                    listOf(
                        "flv",
                        "matroska",
                        "mov",
                        "mp4",
                        "mpeg",
                        "mpegts",
                        "webm",
                    ),
                allValues =
                    listOf(
                        "asf",
                        "avi",
                        "flv",
                        "hls",
                        "matroska",
                        "mov",
                        "mp4",
                        "mpeg",
                        "mpegts",
                        "mpegvideo",
                        "rm",
                        "webm",
                        "wmv",
                    ),
                displayValues = R.array.container_formats,
                getter = { it.playbackPreferences.directPlayFormatList },
                setter = { prefs, value ->
                    prefs.updatePlaybackPreferences {
                        clearDirectPlayFormat()
                        addAllDirectPlayFormat(value)
                    }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
            )

        val advancedPlaybackPrefs =
            listOf(
                StashSwitchPreference(
                    title = R.string.dpad_skipping,
                    prefKey = R.string.pref_key_skip_with_dpad,
                    defaultValue = true,
                    getter = { it.playbackPreferences.dpadSkipping },
                    setter = { prefs, value ->
                        prefs.updatePlaybackPreferences { dpadSkipping = value }
                    },
                    summaryOn = R.string.dpad_skipping_summary_on,
                    summaryOff = R.string.dpad_skipping_summary_off,
                ),
                StashSwitchPreference(
                    title = R.string.dpad_skip_indicator,
                    prefKey = R.string.pref_key_show_dpad_skip,
                    defaultValue = true,
                    getter = { it.interfacePreferences.dpadSkipIndicator },
                    setter = { prefs, value ->
                        prefs.updateInterfacePreferences { dpadSkipIndicator = value }
                    },
                    summaryOn = R.string.stashapp_actions_show,
                    summaryOff = R.string.stashapp_actions_hide,
                ),
                StashChoicePreference<StreamChoice>(
                    title = R.string.stream_choice,
                    prefKey = R.string.pref_key_stream_choice,
                    summary = R.string.stream_choice_summary,
                    defaultValue = StreamChoice.HLS,
                    displayValues = R.array.stream_options,
                    indexToValue = { StreamChoice.forNumber(it) },
                    valueToIndex = { it.number },
                    getter = { it.playbackPreferences.streamChoice },
                    setter = { prefs, value ->
                        prefs.updatePlaybackPreferences { streamChoice = value }
                    },
                    prefGetter = { context: Context, prefs: SharedPreferences ->
                        val value =
                            prefs.getString(
                                context.getString(R.string.pref_key_stream_choice),
                                null,
                            )
                        value?.let { StreamChoice.valueOf(it.uppercase()) } ?: StreamChoice.HLS
                    },
                    prefSetter = { context: Context, editor: SharedPreferences.Editor, value: StreamChoice ->
                        editor.putString(
                            context.getString(R.string.pref_key_stream_choice),
                            value.name.lowercase(),
                        )
                    },
                ),
                DirectPlayVideo,
                DirectPlayAudio,
                DirectPlayFormat,
                StashSwitchPreference(
                    title = R.string.playback_debug_info,
                    prefKey = R.string.pref_key_show_playback_debug_info,
                    defaultValue = false,
                    getter = { it.playbackPreferences.showDebugInfo },
                    setter = { prefs, value ->
                        prefs.updatePlaybackPreferences { showDebugInfo = value }
                    },
                    summaryOn = R.string.stashapp_actions_show,
                    summaryOff = R.string.stashapp_actions_hide,
                ),
                StashSliderPreference(
                    title = R.string.hide_controller_timeout,
                    prefKey = R.string.pref_key_controller_timeout,
                    defaultValue = 3500,
                    min = 500,
                    max = 15.seconds.inWholeMilliseconds.toInt(),
                    interval = 100,
                    getter = { it.playbackPreferences.controllerTimeoutMs },
                    setter = { prefs, value ->
                        prefs.updatePlaybackPreferences { controllerTimeoutMs = value }
                    },
                    toSharedPrefs = { it },
                    fromSharedPrefs = { it },
                    summarizer = { value -> value?.let { "${value / 1000.0} seconds" } },
                ),
                StashSwitchPreference(
                    title = R.string.save_play_history,
                    prefKey = R.string.pref_key_playback_track_activity,
                    defaultValue = true,
                    getter = { it.playbackPreferences.savePlayHistory },
                    setter = { prefs, value ->
                        prefs.updatePlaybackPreferences { savePlayHistory = value }
                    },
                    summaryOn = R.string.save_play_history_summary_on,
                    summaryOff = R.string.transcode_options_disabled,
                ),
                StashSwitchPreference(
                    title = R.string.start_playback_without_audio,
                    prefKey = R.string.pref_key_playback_start_muted,
                    defaultValue = false,
                    getter = { it.playbackPreferences.startPlaybackMuted },
                    setter = { prefs, value ->
                        prefs.updatePlaybackPreferences { startPlaybackMuted = value }
                    },
                    summaryOn = R.string.playback_start_muted_summary_on,
                    summaryOff = R.string.playback_start_muted_summary_off,
                ),
                StashChoicePreference<Resolution>(
                    title = R.string.transcode_above_resolution,
                    prefKey = R.string.pref_key_playback_always_transcode,
                    summary = null,
                    defaultValue = Resolution.UNSPECIFIED,
                    displayValues = R.array.transcode_options,
                    indexToValue = { Resolution.forNumber(it) },
                    valueToIndex = { it.number },
                    getter = { it.playbackPreferences.transcodeAboveResolution },
                    setter = { prefs, value ->
                        prefs.updatePlaybackPreferences { transcodeAboveResolution = value }
                    },
                    prefGetter = { context: Context, prefs: SharedPreferences ->
                        val value =
                            prefs.getString(
                                context.getString(R.string.pref_key_playback_always_transcode),
                                null,
                            )
                        value?.let { Resolution.valueOf(it.uppercase()) }
                            ?: Resolution.UNSPECIFIED
                    },
                    prefSetter = { context: Context, editor: SharedPreferences.Editor, value: Resolution ->
                        editor.putString(
                            context.getString(R.string.pref_key_playback_always_transcode),
                            value.name.lowercase(),
                        )
                    },
                ),
            )

        val VideoFilter =
            StashSwitchPreference(
                title = R.string.enable_video_filters,
                prefKey = R.string.pref_key_video_filters,
                defaultValue = false,
                getter = { it.playbackPreferences.videoFiltersEnabled },
                setter = { prefs, value ->
                    prefs.updatePlaybackPreferences { videoFiltersEnabled = value }
                },
                summaryOn = R.string.stashapp_actions_enable,
                summaryOff = R.string.transcode_options_disabled,
            )

        val PersistVideoFilter =
            StashSwitchPreference(
                title = R.string.persist_video_filters,
                prefKey = R.string.pref_key_playback_save_effects,
                defaultValue = false,
                getter = { it.playbackPreferences.saveVideoFilters },
                setter = { prefs, value ->
                    prefs.updatePlaybackPreferences { saveVideoFilters = value }
                },
                summary = R.string.persist_video_filters_summary,
            )

        val SearchResults =
            StashSliderPreference(
                title = R.string.search_results_title,
                prefKey = R.string.pref_key_max_search_results,
                defaultValue = 25,
                min = 1,
                max = 50,
                interval = 1,
                getter = { it.searchPreferences.maxResults },
                setter = { prefs, value ->
                    prefs.updateSearchPreferences { maxResults = value }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
            )

        val SearchDelay =
            StashSliderPreference(
                title = R.string.search_delay,
                prefKey = R.string.pref_key_search_delay,
                defaultValue = 750,
                min = 0,
                max = 5.seconds.inWholeMilliseconds.toInt(),
                interval = 50,
                getter = { it.searchPreferences.searchDelayMs.toInt() },
                setter = { prefs, value ->
                    prefs.updateSearchPreferences { searchDelayMs = value.toLong() }
                },
                summarizer = { "${it}ms" },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
            )

        val NetworkCache =
            StashSliderPreference(
                title = R.string.network_cache_size,
                prefKey = R.string.pref_key_network_cache_size,
                defaultValue = 100,
                min = 25,
                max = 500,
                interval = 25,
                getter = { (it.cachePreferences.networkCacheSize / (1024.0 * 1024.0)).toInt() },
                setter = { prefs, value ->
                    prefs.updateCachePreferences {
                        networkCacheSize = (value * 1024.0 * 1024.0).toLong()
                    }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
                summarizer = { "${it}MB" },
            )

        val ImageDiskCache =
            StashSliderPreference(
                title = R.string.image_disk_cache_size,
                prefKey = R.string.pref_key_image_cache_size,
                defaultValue = 100,
                min = 25,
                max = 500,
                interval = 25,
                getter = { (it.cachePreferences.imageDiskCacheSize / (1024.0 * 1024.0)).toInt() },
                setter = { prefs, value ->
                    prefs.updateCachePreferences {
                        imageDiskCacheSize = (value * 1024.0 * 1024.0).toLong()
                    }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
                summarizer = { "${it}MB" },
            )

        val CacheInvalidation =
            StashSliderPreference(
                title = R.string.cache_invalidation,
                prefKey = R.string.pref_key_network_cache_duration,
                defaultValue = 6,
                min = 0,
                max = 10,
                interval = 1,
                getter = { it.cachePreferences.cacheExpirationTime },
                setter = { prefs, value ->
                    prefs.updateCachePreferences { cacheExpirationTime = value }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
                summarizer = {
                    cacheDurationPrefToDuration(it ?: 0)?.toString() ?: "Always request from server"
                },
            )

        val CacheLogging =
            StashSwitchPreference(
                title = R.string.cache_logging,
                prefKey = R.string.pref_key_network_cache_logging,
                defaultValue = false,
                getter = { it.cachePreferences.logCacheHits },
                setter = { prefs, value ->
                    prefs.updateCachePreferences { logCacheHits = value }
                },
                summaryOn = R.string.cache_logging_summary_on,
                summaryOff = R.string.cache_logging_summary_off,
            )

        val CacheClear =
            StashClickablePreference(
                title = R.string.clear_cache,
                summary = R.string.clear_cache_summary,
                getter = { },
                setter = { prefs, _ -> prefs },
            )

        val CheckForUpdates =
            StashSwitchPreference(
                title = R.string.check_for_updates,
                prefKey = R.string.pref_key_auto_check_updates,
                defaultValue = true,
                getter = { it.updatePreferences.checkForUpdates },
                setter = { prefs, value ->
                    prefs.updateUpdatePreferences { checkForUpdates = value }
                },
                summaryOn = R.string.stashapp_actions_enable,
                summaryOff = R.string.transcode_options_disabled,
            )

        val UpdateUrl =
            StashStringPreference(
                title = R.string.update_url,
                prefKey = R.string.pref_key_update_url,
                defaultValue = "https://api.github.com/repos/damontecres/StashAppAndroidTV/releases/latest",
                getter = { it.updatePreferences.updateUrl },
                setter = { prefs, value ->
                    prefs.updateUpdatePreferences { updateUrl = value }
                },
                summary = R.string.update_url_summary,
            )

        val OssLicenseInfo =
            StashDestinationPreference(
                title = R.string.oss_license_info,
                destination = Destination.LicenseInfo,
            )

        val CrashReporting =
            StashSwitchPreference(
                title = R.string.crash_reporting,
                prefKey = R.string.pref_key_acra_enable,
                defaultValue = true,
                getter = {
                    PreferenceManager
                        .getDefaultSharedPreferences(StashApplication.getApplication())
                        .getBoolean("acra.enable", true)
                },
                setter = { prefs, value ->
                    PreferenceManager
                        .getDefaultSharedPreferences(StashApplication.getApplication())
                        .edit(true) {
                            putBoolean("acra.enable", value)
                        }
                    prefs
                },
                summaryOn = R.string.stashapp_actions_enable,
                summaryOff = R.string.transcode_options_disabled,
            )

        val LogErrorsToServer =
            StashSwitchPreference(
                title = R.string.log_errors_to_server,
                prefKey = R.string.pref_key_log_to_server,
                defaultValue = true,
                getter = { it.advancedPreferences.logErrorsToServer },
                setter = { prefs, value ->
                    prefs.updateAdvancedPreferences { logErrorsToServer = value }
                },
                summary = R.string.log_errors_to_server_summary,
            )

        val ExperimentalFeatures =
            StashSwitchPreference(
                title = R.string.experimental_features,
                prefKey = R.string.pref_key_experimental_features,
                defaultValue = false,
                getter = { it.advancedPreferences.experimentalFeaturesEnabled },
                setter = { prefs, value ->
                    prefs.updateAdvancedPreferences { experimentalFeaturesEnabled = value }
                },
                summaryOn = R.string.stashapp_actions_enable,
                summaryOff = R.string.transcode_options_disabled,
            )

        val NetworkTimeout =
            StashSliderPreference(
                title = R.string.network_timeout,
                prefKey = R.string.pref_key_network_timeout,
                defaultValue = 15,
                min = 0,
                max = 120,
                interval = 5,
                getter = { (it.advancedPreferences.networkTimeoutMs.milliseconds.inWholeSeconds).toInt() },
                setter = { prefs, value ->
                    prefs.updateAdvancedPreferences {
                        networkTimeoutMs = value.seconds.inWholeMilliseconds
                    }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
                summarizer = { value -> if (value == 0) "Never" else value?.let { "$value seconds" } },
            )

        val PlaybackDebugLogging =
            StashSwitchPreference(
                title = R.string.playback_debug_logging,
                prefKey = R.string.pref_key_playback_debug_logging,
                defaultValue = false,
                getter = { it.playbackPreferences.debugLoggingEnabled },
                setter = { prefs, value ->
                    prefs.updatePlaybackPreferences { debugLoggingEnabled = value }
                },
                summaryOn = R.string.stashapp_actions_enable,
                summaryOff = R.string.transcode_options_disabled,
            )

        val PlaybackStreamingClient =
            StashChoicePreference<PlaybackHttpClient>(
                title = R.string.playback_http_client,
                prefKey = R.string.pref_key_playback_http_client,
                defaultValue = PlaybackHttpClient.OKHTTP,
                displayValues = R.array.playback_http_client,
                indexToValue = { PlaybackHttpClient.forNumber(it) },
                valueToIndex = { it.number },
                getter = { it.playbackPreferences.playbackHttpClient },
                setter = { prefs, value ->
                    prefs.updatePlaybackPreferences { playbackHttpClient = value }
                },
                prefGetter = { context: Context, prefs: SharedPreferences ->
                    val value =
                        prefs.getString(
                            context.getString(R.string.pref_key_playback_http_client),
                            null,
                        )
                    value?.let { PlaybackHttpClient.valueOf(it.uppercase()) }
                        ?: PlaybackHttpClient.OKHTTP
                },
                prefSetter = { context: Context, editor: SharedPreferences.Editor, value: PlaybackHttpClient ->
                    editor.putString(
                        context.getString(R.string.pref_key_playback_http_client),
                        value.name.lowercase(),
                    )
                },
            )

        val ImageThreads =
            StashSliderPreference(
                title = R.string.image_loading_threads,
                prefKey = R.string.pref_key_image_loading_threads,
                defaultValue = Runtime.getRuntime().availableProcessors(),
                min = 1,
                max = Runtime.getRuntime().availableProcessors() * 2,
                interval = 1,
                getter = { it.advancedPreferences.imageThreadCount },
                setter = { prefs, value ->
                    prefs.updateAdvancedPreferences { imageThreadCount = value }
                },
                toSharedPrefs = { it },
                fromSharedPrefs = { it },
                summarizer = {
                    "$it threads, default is ${
                        Runtime.getRuntime().availableProcessors()
                    }"
                },
            )

        val TrustCertificates =
            StashSwitchPreference(
                title = R.string.trust_certificates,
                prefKey = R.string.pref_key_trust_certs,
                defaultValue = false,
                getter = { it.advancedPreferences.trustSelfSignedCertificates },
                setter = { prefs, value ->
                    prefs.updateAdvancedPreferences { trustSelfSignedCertificates = value }
                },
                summary = R.string.trust_certificates_summary,
            )

        val TriggerScan =
            StashClickablePreference(
                title = R.string.trigger_scan,
                summary = R.string.stashapp_config_tasks_scan_for_content_desc,
            )

        val TriggerGenerate =
            StashClickablePreference(
                title = R.string.trigger_generate,
                summary = R.string.stashapp_config_tasks_generate_desc,
            )

        val MigratePreferences =
            StashClickablePreference(
                title = R.string.migrate_preferences,
                summary = R.string.migrate_preferences_summary,
            )
    }
}

data class StashSwitchPreference(
    @get:StringRes override val title: Int,
    @param:StringRes override val prefKey: Int,
    override val defaultValue: Boolean,
    override val getter: (prefs: StashPreferences) -> Boolean,
    override val setter: (prefs: StashPreferences, value: Boolean) -> StashPreferences,
    val validator: (value: Boolean) -> PreferenceValidation = { PreferenceValidation.Valid },
    @param:StringRes val summary: Int? = null,
    @param:StringRes val summaryOn: Int? = null,
    @param:StringRes val summaryOff: Int? = null,
) : StashPreference<Boolean> {
    override val prefGetter = { context: Context, prefs: SharedPreferences ->
        prefs.getBoolean(context.getString(prefKey), defaultValue)
    }
    override val prefSetter = { context: Context, prefs: SharedPreferences.Editor, value: Boolean ->
        prefs.putBoolean(context.getString(prefKey), value)
    }

    override fun summary(
        context: Context,
        value: Boolean?,
    ): String? =
        when {
            summaryOn != null && value == true -> context.getString(summaryOn)
            summaryOff != null && value == false -> context.getString(summaryOff)
            else -> summary?.let { context.getString(summary) }
        }
}

open class StashStringPreference(
    @param:StringRes override val title: Int,
    @param:StringRes override val prefKey: Int,
    override val defaultValue: String,
    override val getter: (StashPreferences) -> String,
    override val setter: (StashPreferences, String) -> StashPreferences,
    @param:StringRes val summary: Int?,
) : StashPreference<String> {
    override val prefGetter = { context: Context, prefs: SharedPreferences ->
        prefs.getString(context.getString(prefKey), null) ?: defaultValue
    }
    override val prefSetter = { context: Context, prefs: SharedPreferences.Editor, value: String ->
        prefs.putString(context.getString(prefKey), value)
    }

    override fun summary(
        context: Context,
        value: String?,
    ): String? = summary?.let { context.getString(it) } ?: value
}

class StashPinPreference(
    @StringRes title: Int,
    @StringRes prefKey: Int,
    defaultValue: String = "",
    @param:StringRes val description: Int,
    override val getter: (prefs: StashPreferences) -> String,
    override val setter: (prefs: StashPreferences, value: String) -> StashPreferences,
) : StashStringPreference(
        title,
        prefKey,
        defaultValue,
        getter,
        setter,
        null,
    ) {
    override fun validate(value: String): PreferenceValidation =
        if (value.isBlank() || value.toIntOrNull() != null) {
            PreferenceValidation.Valid
        } else {
            PreferenceValidation.Invalid("Invalid PIN code format")
        }

    override fun summary(
        context: Context,
        value: String?,
    ): String? =
        if (value.isNotNullOrBlank()) {
            "Pin code set"
        } else {
            "No pin code set"
        }
}

data class StashChoicePreference<T>(
    @param:StringRes override val title: Int,
    @param:StringRes override val prefKey: Int,
    override val defaultValue: T,
    @param:ArrayRes val displayValues: Int,
    val indexToValue: (index: Int) -> T,
    val valueToIndex: (T) -> Int,
    override val getter: (prefs: StashPreferences) -> T,
    override val setter: (prefs: StashPreferences, value: T) -> StashPreferences,
    override val prefGetter: (Context, SharedPreferences) -> T,
    override val prefSetter: (Context, SharedPreferences.Editor, T) -> SharedPreferences.Editor,
    @param:StringRes val summary: Int? = null,
) : StashPreference<T>

data class StashMultiChoicePreference<T>(
    @param:StringRes override val title: Int,
    @param:StringRes override val prefKey: Int,
    override val defaultValue: List<T>,
    val allValues: List<T>,
    @param:ArrayRes val displayValues: Int,
//    val indexToValue: (index: Int) -> T,
//    val valueToIndex: (T) -> Int,
    override val getter: (prefs: StashPreferences) -> List<T>,
    override val setter: (prefs: StashPreferences, value: List<T>) -> StashPreferences,
    @param:StringRes val summary: Int? = null,
    val toSharedPrefs: (T) -> String,
    val fromSharedPrefs: (String) -> T,
) : StashPreference<List<T>> {
    override val prefGetter = { context: Context, prefs: SharedPreferences ->
        prefs.getStringSet(context.getString(prefKey), null)?.map(fromSharedPrefs) ?: defaultValue
    }
    override val prefSetter =
        { context: Context, editor: SharedPreferences.Editor, value: List<T> ->
            val values = value.map(toSharedPrefs)
            editor.putStringSet(context.getString(prefKey), values.toSet())
        }
}

data class StashClickablePreference(
    @param:StringRes override val title: Int,
    override val defaultValue: Unit = Unit,
    override val getter: (prefs: StashPreferences) -> Unit = { },
    override val setter: (prefs: StashPreferences, value: Unit) -> StashPreferences = { prefs, _ -> prefs },
    override val prefGetter: (Context, SharedPreferences) -> Unit = { _, _ -> },
    override val prefSetter: (Context, SharedPreferences.Editor, Unit) -> SharedPreferences.Editor = { _, editor, _ -> editor },
    @param:StringRes val summary: Int? = null,
    @param:StringRes override val prefKey: Int = 0,
) : StashPreference<Unit> {
    override fun summary(
        context: Context,
        value: Unit?,
    ): String? = summary?.let { context.getString(it) }
}

data class StashDestinationPreference(
    @param:StringRes override val title: Int,
    override val defaultValue: Unit = Unit,
    override val getter: (prefs: StashPreferences) -> Unit = { },
    override val setter: (prefs: StashPreferences, value: Unit) -> StashPreferences = { prefs, _ -> prefs },
    override val prefGetter: (Context, SharedPreferences) -> Unit = { _, _ -> },
    override val prefSetter: (Context, SharedPreferences.Editor, Unit) -> SharedPreferences.Editor = { _, editor, _ -> editor },
    @param:StringRes val summary: Int? = null,
    @param:StringRes override val prefKey: Int = 0,
    val destination: Destination,
) : StashPreference<Unit> {
    override fun summary(
        context: Context,
        value: Unit?,
    ): String? = summary?.let { context.getString(it) }
}

class StashSliderPreference(
    @param:StringRes override val title: Int,
    @param:StringRes override val prefKey: Int,
    override val defaultValue: Int,
    val min: Int = 0,
    val max: Int = 100,
    val interval: Int = 1,
    override val getter: (prefs: StashPreferences) -> Int,
    override val setter: (prefs: StashPreferences, value: Int) -> StashPreferences,
    val toSharedPrefs: (Int) -> Int,
    val fromSharedPrefs: (Int) -> Int,
    @param:StringRes val summary: Int? = null,
    val summarizer: ((Int?) -> String?)? = null,
) : StashPreference<Int> {
    override val prefGetter = { context: Context, prefs: SharedPreferences ->
        val value = prefs.getInt(context.getString(prefKey), -1).takeIf { it >= 0 }
        val result = value?.let { fromSharedPrefs.invoke(it) } ?: defaultValue
//        Log.d("SharedPreferencesListener", "prefGetter for $prefKey: $value => $result")
        result
    }
    override val prefSetter = { context: Context, prefs: SharedPreferences.Editor, value: Int ->
        val newValue = toSharedPrefs.invoke(value)
//        Log.d("SharedPreferencesListener", "prefGetter for $prefKey: $value => $newValue")
        prefs.putInt(context.getString(prefKey), toSharedPrefs.invoke(value))
    }

    override fun summary(
        context: Context,
        value: Int?,
    ): String? =
        summarizer?.invoke(value)
            ?: summary?.let { context.getString(it) }
            ?: value?.toString()
}
