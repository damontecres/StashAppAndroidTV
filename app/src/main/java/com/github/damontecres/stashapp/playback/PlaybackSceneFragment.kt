package com.github.damontecres.stashapp.playback

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.preference.PreferenceManager
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.ui.components.prefs.StashPreference
import com.github.damontecres.stashapp.util.preferences
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.Constants
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashServer

@OptIn(UnstableApi::class)
class PlaybackSceneFragment : PlaybackFragment() {
    override val previewsEnabled: Boolean
        get() = true
    override val optionsButtonOptions: OptionsButtonOptions
        get() = OptionsButtonOptions(DataType.SCENE, false)

    override fun Player.setupPlayer() {
        repeatMode = Player.REPEAT_MODE_OFF
    }

    @OptIn(UnstableApi::class)
    override fun Player.postSetupPlayer() {
        currentScene?.let {
            maybeAddActivityTracking(it)
        }
        val finishedBehavior =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getString(
                    requireContext().getString(R.string.pref_key_playback_finished_behavior),
                    getString(R.string.playback_finished_do_nothing),
                )
        when (finishedBehavior) {
            getString(R.string.playback_finished_repeat) -> {
                repeatMode = Player.REPEAT_MODE_ONE
            }

            getString(R.string.playback_finished_return) -> {
                StashExoPlayer.addListener(
                    object :
                        Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                setFragmentResult(
                                    Constants.POSITION_REQUEST_KEY,
                                    Bundle().apply { putLong(Constants.POSITION_REQUEST_KEY, 0L) },
                                )
                                StashApplication.navigationManager.goBack()
                            }
                        }
                    },
                )
            }

            getString(R.string.playback_finished_do_nothing) -> {
                StashExoPlayer.addListener(
                    object :
                        Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                videoView.showController()
                            }
                        }
                    },
                )
            }

            else -> {
                Log.w(TAG, "Unknown playbackFinishedBehavior: $finishedBehavior")
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val playback = requireArguments().getDestination<Destination.Playback>()
        viewModel.setScene(playback.sceneId)

        viewModel.scene.observe(viewLifecycleOwner) { scene ->
            currentScene = scene
            if (scene == null) {
                return@observe
            }
            maybeAddActivityTracking(scene)
            val position =
                if (playbackPosition >= 0) {
                    playbackPosition
                } else {
                    playback.position
                }
            val streamChoice = getStreamChoiceFromPreferences(requireContext())
            val transcodeResolution = getTranscodeAboveFromPreferences(requireContext())
            Log.d(TAG, "playbackPosition=$playbackPosition, playback.position=${playback.position}")
            val streamDecision =
                getStreamDecision(
                    requireContext(),
                    scene,
                    playback.mode,
                    streamChoice,
                    transcodeResolution,
                )
            Log.d(TAG, "streamDecision=$streamDecision")
            updateDebugInfo(streamDecision, scene)

            player!!.also { exoPlayer ->
                if (scene.streams.isNotEmpty()) {
                    val funscriptUrl = scene.funscriptUrl
                    if (exoPlayer is ExoPlayer) {
                        maybeSetupVideoEffects(exoPlayer)
                    }
                    exoPlayer.setMediaItem(
                        buildMediaItem(requireContext(), streamDecision, scene),
                        if (position > 0) position else C.TIME_UNSET,
                    )
                    exoPlayer.volume = 1f
                    maybeMuteAudio(requireContext(), false, exoPlayer)
                    if (videoView.controllerShowTimeoutMs > 0) {
                        videoView.hideController()
                    }

                    if (scene.interactive &&
                        com.github.damontecres.stashapp.util.HandyManager.isHandyEnabled
                    ) {
                        lifecycleScope.launch {
                            try {
                                exoPlayer.playWhenReady = false
                                var currentUrl = scene.funscriptUrl

                                // If blank/null, try duplicate scenes
                                if (currentUrl.isNullOrBlank()) {
                                    val queryEngine = QueryEngine(StashServer.requireCurrentServer())
                                    val dupUrl = queryEngine.findDuplicateFunscriptUrl(scene.id, scene.fingerprints)
                                    if (!dupUrl.isNullOrBlank()) {
                                        currentUrl = dupUrl
                                    }
                                }

                                if (!currentUrl.isNullOrBlank()) {
                                    Toast.makeText(requireContext(), R.string.funscript_loading, Toast.LENGTH_SHORT).show()

                                    val isLocalIp = currentUrl.contains("//192.168.") ||
                                        currentUrl.contains("//10.") ||
                                        currentUrl.contains("//172.") ||
                                        currentUrl.contains("//localhost") ||
                                        currentUrl.contains("//127.0.0.1")

                                    if (isLocalIp) {
                                        Toast.makeText(requireContext(), R.string.handy_cloud_bridge_uploading, Toast.LENGTH_SHORT).show()
                                    }

                                    val initialUrl = currentUrl
                                    var result =
                                        withTimeoutOrNull(30000L) {
                                            com.github.damontecres.stashapp.util.HandyManager.initialize(requireContext())
                                            com.github.damontecres.stashapp.util.HandyManager.setup(initialUrl)
                                        } ?: com.github.damontecres.stashapp.util.HandyManager.HandyResult.GenericError("Timeout")

                                    // If failed and we played from primary URL, try duplicate
                                    if (result !is com.github.damontecres.stashapp.util.HandyManager.HandyResult.Success && scene.funscriptUrl == currentUrl) {
                                        val queryEngine = QueryEngine(StashServer.requireCurrentServer())
                                        val dupUrl = queryEngine.findDuplicateFunscriptUrl(scene.id, scene.fingerprints)
                                        if (!dupUrl.isNullOrBlank()) {
                                            currentUrl = dupUrl
                                            val backupUrl = currentUrl
                                            result = withTimeoutOrNull(30000L) {
                                                com.github.damontecres.stashapp.util.HandyManager.setup(backupUrl)
                                            } ?: com.github.damontecres.stashapp.util.HandyManager.HandyResult.GenericError("Timeout")
                                        }
                                    }

                                    if (result is com.github.damontecres.stashapp.util.HandyManager.HandyResult.Success) {
                                        Toast.makeText(requireContext(), R.string.funscript_success, Toast.LENGTH_SHORT).show()
                                        Log.i(TAG, "Handy setup successful")
                                        StashExoPlayer.addListener(
                                            object : Player.Listener {
                                                override fun onIsPlayingChanged(isPlaying: Boolean) {
                                                    if (isPlaying) {
                                                        com.github.damontecres.stashapp.util.HandyManager.play(exoPlayer.currentPosition)
                                                    } else {
                                                        com.github.damontecres.stashapp.util.HandyManager.stop()
                                                    }
                                                }

                                                override fun onPositionDiscontinuity(
                                                    oldPosition: Player.PositionInfo,
                                                    newPosition: Player.PositionInfo,
                                                    reason: Int,
                                                ) {
                                                    if (exoPlayer.isPlaying) {
                                                        com.github.damontecres.stashapp.util.HandyManager.play(newPosition.positionMs)
                                                    }
                                                }
                                            },
                                        )
                                    } else {
                                        val errorMsg = result.toString()
                                        Log.e(TAG, "Handy setup failed: $errorMsg")

                                        val displayUrl = currentUrl
                                        withContext(Dispatchers.Main) {
                                            val errorText = "The Handy cloud could not process the funscript.\n\nError: $errorMsg\n\nURL: $displayUrl\n\nNote: If you use a local IP, the Handy cloud cannot reach it."
                                            val builder = AlertDialog.Builder(requireContext())
                                            builder.setTitle("Handy Funscript Error")
                                            builder.setMessage(errorText)
                                            builder.setPositiveButton(android.R.string.ok, null)
                                            builder.setNeutralButton("Copy Error") { _, _ ->
                                                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("Handy Error", errorText)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                            builder.show()
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Handy setup error", e)
                                Toast.makeText(requireContext(), R.string.funscript_error, Toast.LENGTH_LONG).show()
                            } finally {
                                exoPlayer.playWhenReady = wasPlayingBeforeResultLauncher ?: true
                            }
                        }
                    }

                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = wasPlayingBeforeResultLauncher ?: true
                } else {
                    videoView.useController = false
                    Toast
                        .makeText(
                            requireContext(),
                            "This scene has no video files to play",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }

        val handyToggleButton = view.findViewById<android.widget.ImageButton>(R.id.handy_toggle_button)
        if (handyToggleButton != null) {
            val scene = viewModel.scene.value
            if (scene?.interactive == true) {
                handyToggleButton.visibility = View.VISIBLE
                com.github.damontecres.stashapp.util.HandyManager.initialize(requireContext())
                val updateHandyIcon = {
                    if (com.github.damontecres.stashapp.util.HandyManager.isHandyEnabled) {
                        handyToggleButton.setColorFilter(android.graphics.Color.WHITE)
                        handyToggleButton.alpha = 1.0f
                    } else {
                        handyToggleButton.setColorFilter(android.graphics.Color.GRAY)
                        handyToggleButton.alpha = 0.5f
                    }
                }
                updateHandyIcon()
                handyToggleButton.setOnClickListener {
                    com.github.damontecres.stashapp.util.HandyManager.initialize(requireContext())
                    val enabled = !com.github.damontecres.stashapp.util.HandyManager.isHandyEnabled
                    com.github.damontecres.stashapp.util.HandyManager.isHandyEnabled = enabled
                    updateHandyIcon()
                    lifecycleScope.launch {
                        requireContext().preferences.updateData { prefs ->
                            StashPreference.HandyEnabled.setter(prefs, enabled)
                        }
                    }
                    if (enabled) {
                        // Attempt to reconnect if enabled inline
                        lifecycleScope.launch {
                            var currentUrl = scene.funscriptUrl

                            // If blank/null, try duplicate scenes
                            if (currentUrl.isNullOrBlank()) {
                                val queryEngine = QueryEngine(StashServer.requireCurrentServer())
                                val dupUrl = queryEngine.findDuplicateFunscriptUrl(scene.id, scene.fingerprints)
                                if (!dupUrl.isNullOrBlank()) {
                                    currentUrl = dupUrl
                                }
                            }

                            if (!currentUrl.isNullOrBlank()) {
                                Toast.makeText(requireContext(), R.string.funscript_loading, Toast.LENGTH_SHORT).show()
                                val exoPlayer = player as ExoPlayer
                                val wasPlaying = exoPlayer.playWhenReady
                                try {
                                    exoPlayer.playWhenReady = false
                                    val initialUrl = currentUrl
                                    var result = withTimeoutOrNull(30000L) {
                                        com.github.damontecres.stashapp.util.HandyManager.setup(initialUrl)
                                    }

                                    // If failed and we played from primary URL, try duplicate
                                    if (result !is com.github.damontecres.stashapp.util.HandyManager.HandyResult.Success && scene.funscriptUrl == currentUrl) {
                                        val queryEngine = QueryEngine(StashServer.requireCurrentServer())
                                        val dupUrl = queryEngine.findDuplicateFunscriptUrl(scene.id, scene.fingerprints)
                                        if (!dupUrl.isNullOrBlank()) {
                                            currentUrl = dupUrl
                                            val backupUrl = currentUrl
                                            result = withTimeoutOrNull(30000L) {
                                                com.github.damontecres.stashapp.util.HandyManager.setup(backupUrl)
                                            }
                                        }
                                    }

                                    if (result is com.github.damontecres.stashapp.util.HandyManager.HandyResult.Success) {
                                        Toast.makeText(requireContext(), R.string.funscript_success, Toast.LENGTH_SHORT).show()
                                        if (wasPlaying) {
                                            com.github.damontecres.stashapp.util.HandyManager.play(exoPlayer.currentPosition)
                                        }
                                    } else {
                                        Toast.makeText(requireContext(), R.string.funscript_error, Toast.LENGTH_SHORT).show()
                                    }
                                } finally {
                                    exoPlayer.playWhenReady = wasPlaying
                                    // Re-sync icon with actual state (Handy may still be enabled)
                                    updateHandyIcon()
                                }
                            }
                        }
                    } else {
                        com.github.damontecres.stashapp.util.HandyManager.stop()
                    }
                }
            } else {
                handyToggleButton.visibility = View.GONE
            }
        }
    }

    companion object {
        const val TAG = "PlaybackSceneFragment"
    }
}
