package com.github.damontecres.stashapp.util

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.github.damontecres.stashapp.proto.AdvancedPreferences
import com.github.damontecres.stashapp.proto.CachePreferences
import com.github.damontecres.stashapp.proto.InterfacePreferences
import com.github.damontecres.stashapp.proto.PinPreferences
import com.github.damontecres.stashapp.proto.PlaybackPreferences
import com.github.damontecres.stashapp.proto.SearchPreferences
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.proto.StreamChoice
import com.github.damontecres.stashapp.proto.TabPreferences
import com.github.damontecres.stashapp.proto.UpdatePreferences
import com.github.damontecres.stashapp.ui.components.prefs.StashPreference
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration.Companion.seconds

object StashPreferencesSerializer : Serializer<StashPreferences> {
    override val defaultValue: StashPreferences =
        StashPreferences
            .newBuilder()
            .apply {
                interfacePreferences =
                    InterfacePreferences
                        .newBuilder()
                        .apply {
                            useComposeUi = StashPreference.UseNewUI.defaultValue
                            cardSize = StashPreference.CardSize.defaultValue
                            playVideoPreviews = StashPreference.PlayVideoPreviews.defaultValue
                            rememberSelectedTab = StashPreference.RememberTab.defaultValue
                            cardPreviewDelayMs =
                                StashPreference.VideoPreviewDelay.defaultValue.toLong()
                            slideShowIntervalMs =
                                StashPreference.SlideshowDuration.defaultValue.seconds.inWholeMilliseconds
                            slideShowImageClipPauseMs =
                                StashPreference.SlideshowImageClipDelay.defaultValue.toLong()
                            showGridJumpButtons = StashPreference.GridJumpButtons.defaultValue
                            theme = "default"
                            themeStyle = StashPreference.ThemeStylePref.defaultValue
                            showProgressWhenSkipping =
                                StashPreference.ShowProgressSkipping.defaultValue
                            playMovementSounds = StashPreference.MovementSound.defaultValue
                            captionsByDefault = StashPreference.CaptionsOnByDefault.defaultValue
                            scrollNextViewAll = StashPreference.ScrollViewAll.defaultValue
                            scrollTopOnBack = StashPreference.ScrollTopOnBack.defaultValue
                            showPositionFooter = StashPreference.ShowGridFooter.defaultValue
                            showRatingOnCards = StashPreference.ShowCardRating.defaultValue
                            videoPreviewAudio = StashPreference.PlayCardAudio.defaultValue
                            pageWithRemoteButtons = StashPreference.PageRemoteButtons.defaultValue
                            dpadSkipIndicator = StashPreference.DPadSkipIndicator.defaultValue
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
                                StashPreference.SkipForward.defaultValue.seconds.inWholeMilliseconds
                            skipBackwardMs =
                                StashPreference.SkipBack.defaultValue.seconds.inWholeMilliseconds
                            dpadSkipping = StashPreference.DPadSkipping.defaultValue
                            controllerTimeoutMs = StashPreference.ControllerTimeout.defaultValue
                            savePlayHistory = StashPreference.SavePlayHistory.defaultValue
                            saveVideoFilters = StashPreference.VideoFilter.defaultValue
                            seekBarSteps = 16

                            addAllDirectPlayVideo(StashPreference.DirectPlayVideo.defaultValue)
                            addAllDirectPlayAudio(StashPreference.DirectPlayAudio.defaultValue)
                            addAllDirectPlayFormat(StashPreference.DirectPlayFormat.defaultValue)
                        }.build()
                updatePreferences =
                    UpdatePreferences
                        .newBuilder()
                        .apply {
                            checkForUpdates = StashPreference.AutoCheckForUpdates.defaultValue
                            updateUrl = StashPreference.UpdateUrl.defaultValue
                        }.build()
                advancedPreferences =
                    AdvancedPreferences
                        .newBuilder()
                        .apply {
                            logErrorsToServer = StashPreference.LogErrorsToServer.defaultValue
                            networkTimeoutMs =
                                StashPreference.NetworkTimeout.defaultValue.seconds.inWholeMilliseconds
                            imageThreadCount = StashPreference.ImageThreads.defaultValue
                        }.build()
                cachePreferences =
                    CachePreferences
                        .newBuilder()
                        .apply {
                            imageDiskCacheSize =
                                StashPreference.ImageDiskCache.defaultValue * 1024L * 1024L
                            networkCacheSize =
                                StashPreference.NetworkCache.defaultValue * 1024L * 1024L
                            cacheExpirationTime = StashPreference.CacheInvalidation.defaultValue
                        }.build()
                searchPreferences =
                    SearchPreferences
                        .newBuilder()
                        .apply {
                            maxResults = StashPreference.SearchResults.defaultValue
                            searchDelayMs = StashPreference.SearchDelay.defaultValue.toLong()
                        }.build()
            }.build()

    override suspend fun readFrom(input: InputStream): StashPreferences {
        try {
            return StashPreferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(
        t: StashPreferences,
        output: OutputStream,
    ) = t.writeTo(output)
}

val Context.preferences: DataStore<StashPreferences> by dataStore(
    fileName = "preferences.pb",
    serializer = StashPreferencesSerializer,
)

val StreamChoice.asString: String
    get() =
        when (this) {
            StreamChoice.HLS -> "HLS"
            StreamChoice.DASH -> "DASH"
            StreamChoice.MP4 -> "MP4"
            StreamChoice.WEBM -> "WEBM"
            StreamChoice.UNRECOGNIZED -> "HLS"
        }

inline fun StashPreferences.update(block: StashPreferences.Builder.() -> Unit): StashPreferences = toBuilder().apply(block).build()

inline fun StashPreferences.updateInterfacePreferences(block: InterfacePreferences.Builder.() -> Unit): StashPreferences =
    update {
        interfacePreferences = interfacePreferences.toBuilder().apply(block).build()
    }

inline fun StashPreferences.updateTabPreferences(block: TabPreferences.Builder.() -> Unit): StashPreferences =
    updateInterfacePreferences {
        tabPreferences = tabPreferences.toBuilder().apply(block).build()
    }

inline fun StashPreferences.updatePlaybackPreferences(block: PlaybackPreferences.Builder.() -> Unit): StashPreferences =
    update {
        playbackPreferences = playbackPreferences.toBuilder().apply(block).build()
    }

inline fun StashPreferences.updateAdvancedPreferences(block: AdvancedPreferences.Builder.() -> Unit): StashPreferences =
    update {
        advancedPreferences = advancedPreferences.toBuilder().apply(block).build()
    }

inline fun StashPreferences.updateCachePreferences(block: CachePreferences.Builder.() -> Unit): StashPreferences =
    update {
        cachePreferences = cachePreferences.toBuilder().apply(block).build()
    }

inline fun StashPreferences.updateSearchPreferences(block: SearchPreferences.Builder.() -> Unit): StashPreferences =
    update {
        searchPreferences = searchPreferences.toBuilder().apply(block).build()
    }

inline fun StashPreferences.updateUpdatePreferences(block: UpdatePreferences.Builder.() -> Unit): StashPreferences =
    update {
        updatePreferences = updatePreferences.toBuilder().apply(block).build()
    }

inline fun StashPreferences.updatePinPreferences(block: PinPreferences.Builder.() -> Unit): StashPreferences =
    update {
        pinPreferences = pinPreferences.toBuilder().apply(block).build()
    }
