package com.github.damontecres.stashapp.util

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.github.damontecres.stashapp.proto.AdvancedPreferences
import com.github.damontecres.stashapp.proto.CachePreferences
import com.github.damontecres.stashapp.proto.InterfacePreferences
import com.github.damontecres.stashapp.proto.PlaybackPreferences
import com.github.damontecres.stashapp.proto.SearchPreferences
import com.github.damontecres.stashapp.proto.StashPreferences
import com.github.damontecres.stashapp.proto.StreamChoice
import com.github.damontecres.stashapp.proto.ThemeStyle
import com.github.damontecres.stashapp.proto.UpdatePreferences
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
                            themeStyle = ThemeStyle.THEME_STYLE_DARK
                            showProgressWhenSkipping = true
                            playMovementSounds = true
                            captionsByDefault = true
                            // TODO tab prefs
                            scrollNextViewAll = true
                            scrollTopOnBack = true
                            showPositionFooter = true
                            showRatingOnCards = true
                            videoPreviewAudio = false
                            pageWithRemoteButtons = true
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
                            enableCrashReporting = true
                            logErrorsToServer = true
                            networkTimeoutMs = 15.seconds.inWholeMilliseconds
                            imageThreadCount = Runtime.getRuntime().availableProcessors()
                        }.build()
                cachePreferences =
                    CachePreferences
                        .newBuilder()
                        .apply {
                            // TODO
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
            StreamChoice.STREAM_CHOICE_HLS -> "HLS"
            StreamChoice.STREAM_CHOICE_DASH -> "DASH"
            StreamChoice.STREAM_CHOICE_MP4 -> "MP4"
            StreamChoice.STREAM_CHOICE_WEBM -> "WEBM"
            StreamChoice.UNRECOGNIZED -> "HLS"
        }

inline fun StashPreferences.update(block: StashPreferences.Builder.() -> Unit): StashPreferences = toBuilder().apply(block).build()

inline fun StashPreferences.updateInterfacePreferences(block: InterfacePreferences.Builder.() -> Unit): StashPreferences =
    update {
        interfacePreferences = interfacePreferences.toBuilder().apply(block).build()
    }

inline fun StashPreferences.updatePlaybackPreferences(block: PlaybackPreferences.Builder.() -> Unit): StashPreferences =
    update {
        playbackPreferences = playbackPreferences.toBuilder().apply(block).build()
    }
