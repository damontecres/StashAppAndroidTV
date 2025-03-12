package com.github.damontecres.stashapp.views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.ContentLoadingProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.SceneMarkerUpdateInput
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.util.toLongMilliseconds
import com.github.damontecres.stashapp.views.models.MarkerDetailsViewModel
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.launch

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
        viewModel.init(dest.markerId, true)

        val picker = view.findViewById<DurationPicker2>(R.id.duration_picker)
        val sceneTitle = view.findViewById<TextView>(R.id.scene_title)
        val markerTitle = view.findViewById<TextView>(R.id.marker_title)
        val imageView = view.findViewById<ImageView>(R.id.image_view)
        val progressBar = view.findViewById<ContentLoadingProgressBar>(R.id.loading_progress_bar)

        viewModel.screenshot.observe(viewLifecycleOwner) { imageLoading ->
            when (imageLoading) {
                MarkerDetailsViewModel.ImageLoadState.Initialized -> {
                    viewModel.getImageFor(picker.duration, 500)
                }

                MarkerDetailsViewModel.ImageLoadState.Initializing,
                MarkerDetailsViewModel.ImageLoadState.Loading,
                -> {
                    progressBar.visibility = View.VISIBLE
                    progressBar.show()
                }

                is MarkerDetailsViewModel.ImageLoadState.Success -> {
                    Log.v(TAG, "New image is null: ${imageLoading.image == null}")
                    imageView.setImageBitmap(
                        imageLoading.image,
                    )
                    progressBar.hide()
                }

                is MarkerDetailsViewModel.ImageLoadState.Error -> {
                    Toast
                        .makeText(
                            requireContext(),
                            "Error fetching screenshot: ${imageLoading.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }

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

            sceneTitle.text =
                viewModel.item.value!!
                    .scene.videoSceneData.titleOrFilename
            markerTitle.text =
                "${marker.primary_tag.tagData.name} - ${durationToString(marker.seconds)}"

            picker.setMaxDuration(duration)
            picker.duration = marker.seconds.toLongMilliseconds
            picker.isActivated = true

            picker.addOnValueChangedListener { _, _ ->
                viewModel.getImageFor(picker.duration, 500L)
            }

            picker.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch(
                    StashCoroutineExceptionHandler(
                        autoToast = true,
                    ),
                ) {
                    picker.isActivated = false
                    val seconds = picker.duration.coerceAtLeast(0) / 1000.0
                    val mutationEngine =
                        MutationEngine(StashServer.requireCurrentServer())
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
                            ),
                        )
                    Log.v(TAG, "newSeconds=${result?.seconds}")

                    serverViewModel.navigationManager.goBack()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.release()
    }

    companion object {
        private const val TAG = "MarkerPicker"
    }
}
