package com.github.damontecres.stashapp.views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashExoPlayer
import com.github.damontecres.stashapp.api.type.SceneMarkerUpdateInput
import com.github.damontecres.stashapp.data.Scene
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.playback.PlaybackMode
import com.github.damontecres.stashapp.playback.StashPlayerView
import com.github.damontecres.stashapp.playback.buildMediaItem
import com.github.damontecres.stashapp.playback.getStreamDecision
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.util.toLongMilliseconds
import com.github.damontecres.stashapp.views.models.MarkerDetailsViewModel
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Select a value to shift a Marker's seconds by
 */
class MarkerPickerFragment : Fragment(R.layout.marker_picker) {
    private val serverViewModel by activityViewModels<ServerViewModel>()
    private val viewModel by viewModels<MarkerDetailsViewModel>()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val dest = requireArguments().getDestination<Destination.UpdateMarker>()
        viewModel.init(dest.markerId)

        val picker = view.findViewById<DurationPicker2>(R.id.duration_picker)
        val endPicker = view.findViewById<DurationPicker2>(R.id.end_duration_picker)
        val sceneTitle = view.findViewById<TextView>(R.id.scene_title)
        val markerTitle = view.findViewById<TextView>(R.id.marker_title)
        val playerView = view.findViewById<StashPlayerView>(R.id.player_view)
        val saveEndSwitch = view.findViewById<SwitchCompat>(R.id.save_end_time_switch)
        val playButton = view.findViewById<Button>(R.id.play_button)
        val saveButton = view.findViewById<Button>(R.id.save_button)

        viewModel.item.observe(viewLifecycleOwner) { marker ->
            if (marker == null) {
                return@observe
            }
            val duration =
                marker.scene.videoSceneData.files
                    .firstOrNull()
                    ?.videoFile
                    ?.duration
                    ?.toLongMilliseconds
            if (duration == null) {
                return@observe
            }
            val scene = Scene.fromVideoSceneData(marker.scene.videoSceneData)
            val streamDecision =
                getStreamDecision(requireContext(), scene, PlaybackMode.Choose)
            val mediaItem = buildMediaItem(requireContext(), streamDecision, scene)

            StashExoPlayer.releasePlayer()
            val player =
                StashExoPlayer
                    .getInstance(requireContext(), serverViewModel.requireServer())
            playerView.player = player

            sceneTitle.text =
                viewModel.item.value!!
                    .scene.videoSceneData.titleOrFilename
            val title = "${marker.primary_tag.tagData.name} - ${
                marker.seconds.toLongMilliseconds.toDuration(DurationUnit.MILLISECONDS)
            }"
            markerTitle.text = title

            saveEndSwitch.isChecked = marker.end_seconds != null

            picker.setMaxDuration(duration)
            picker.duration = marker.seconds.toLongMilliseconds
            picker.isActivated = false
            endPicker.setMaxDuration(duration)
            endPicker.duration = (marker.end_seconds ?: marker.seconds).toLongMilliseconds
            endPicker.isActivated = false

            fun setPosition(position: Long) {
                player.setMediaItem(mediaItem, position)
                player.prepare()
            }

            picker.setOnClickListener {
                picker.isActivated = !picker.isActivated
                if (picker.isActivated) {
                    setPosition(picker.duration)
                }
            }
            endPicker.setOnClickListener {
                endPicker.isActivated = !endPicker.isActivated
                if (endPicker.isActivated) {
                    setPosition(endPicker.duration)
                }
            }
            picker.requestFocus()

            setPosition(marker.seconds.toLongMilliseconds)

            var job: Job? = null
            picker.addOnValueChangedListener { _, _ ->
                job?.cancel()
                job =
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        delay(500L)
                        setPosition(picker.duration)
                    }
            }
            endPicker.addOnValueChangedListener { _, _ ->
                job?.cancel()
                job =
                    viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                        delay(500L)
                        setPosition(endPicker.duration)
                    }
            }

            playButton.setOnClickListener {
                if (player.isPlaying) {
                    player.pause()
                    playButton.text = getString(R.string.fa_play)
                } else {
                    player.play()
                    playButton.text = getString(R.string.fa_pause)
                }
            }

            saveButton.setOnClickListener {
                picker.isActivated = false
                endPicker.isActivated = false
                val seconds = picker.duration.coerceAtLeast(0) / 1000.0
                val endSeconds = endPicker.duration.coerceAtLeast(0) / 1000.0

                if (saveEndSwitch.isChecked && seconds > endSeconds) {
                    Toast
                        .makeText(
                            requireContext(),
                            getString(R.string.stashapp_validation_end_time_before_start_time),
                            Toast.LENGTH_SHORT,
                        ).show()
                } else {
                    viewLifecycleOwner.lifecycleScope.launch(
                        StashCoroutineExceptionHandler(
                            autoToast = true,
                        ),
                    ) {
                        val mutationEngine =
                            MutationEngine(StashServer.requireCurrentServer())
                        val newEnd =
                            if (saveEndSwitch.isChecked) {
                                Optional.present(endSeconds)
                            } else {
                                Optional.present(null)
                            }
                        val result =
                            mutationEngine.updateMarker(
                                SceneMarkerUpdateInput(
                                    id = marker.id,
                                    scene_id =
                                        Optional.present(
                                            viewModel.item.value!!
                                                .scene.videoSceneData.id,
                                        ),
                                    seconds = Optional.present(seconds),
                                    end_seconds = newEnd,
                                ),
                            )
                        Log.v(TAG, "newSeconds=${result?.seconds}, newEnd=${result?.end_seconds}")

                        serverViewModel.navigationManager.goBack()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        StashExoPlayer.releasePlayer()
    }

    companion object {
        private const val TAG = "MarkerPicker"
    }
}
