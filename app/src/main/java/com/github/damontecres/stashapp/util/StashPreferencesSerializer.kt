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
import com.github.damontecres.stashapp.proto.ThemeStyle
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
                            useComposeUi = true
                            cardSize = 5
                            playVideoPreviews = true
                            rememberSelectedTab = true
                            cardPreviewDelayMs = 1000
                            slideShowIntervalMs = 5000
                            slideShowImageClipPauseMs = 250
                            showGridJumpButtons = true
                            theme = "default"
                            themeStyle = ThemeStyle.DARK
                            showProgressWhenSkipping = true
                            playMovementSounds = true
                            captionsByDefault = true
                            scrollNextViewAll = true
                            scrollTopOnBack = true
                            showPositionFooter = true
                            showRatingOnCards = true
                            videoPreviewAudio = false
                            pageWithRemoteButtons = true
                            dpadSkipIndicator = true
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
                            skipForwardMs = 30.seconds.inWholeMilliseconds
                            skipBackwardMs = 10.seconds.inWholeMilliseconds
                            dpadSkipping = true
                            controllerTimeoutMs = 3500
                            savePlayHistory = true
                            saveVideoFilters = true

                            addAllDirectPlayVideo(StashPreference.DirectPlayVideo.defaultValue)
                            addAllDirectPlayAudio(StashPreference.DirectPlayAudio.defaultValue)
                            addAllDirectPlayFormat(StashPreference.DirectPlayFormat.defaultValue)
                        }.build()
                updatePreferences =
                    UpdatePreferences
                        .newBuilder()
                        .apply {
                            checkForUpdates = true
                            updateUrl = "https://api.github.com/repos/damontecres/StashAppAndroidTV/releases/latest"
                        }.build()
                advancedPreferences =
                    AdvancedPreferences
                        .newBuilder()
                        .apply {
                            logErrorsToServer = true
                            networkTimeoutMs = 15.seconds.inWholeMilliseconds
                            imageThreadCount = Runtime.getRuntime().availableProcessors()
                        }.build()
                cachePreferences =
                    CachePreferences
                        .newBuilder()
                        .apply {
                            imageDiskCacheSize = 100 * 1024 * 1024 // 100 MB
                            networkCacheSize = 10 * 1024 * 1024 // 10 MB
                            cacheExpirationTime = 6 // TODO
                        }.build()
                searchPreferences =
                    SearchPreferences
                        .newBuilder()
                        .apply {
                            maxResults = 25
                            searchDelayMs = 1000
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
