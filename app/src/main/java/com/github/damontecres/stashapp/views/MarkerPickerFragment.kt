package com.github.damontecres.stashapp.views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.leanback.widget.picker.Picker
import androidx.leanback.widget.picker.PickerColumn
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.SceneMarkerUpdateInput
import com.github.damontecres.stashapp.data.Marker
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.util.titleOrFilename
import com.github.damontecres.stashapp.views.models.MarkerDetailsViewModel
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.launch

/**
 * Select a value to shift a [Marker]'s seconds by
 */
class MarkerPickerFragment : Fragment(R.layout.marker_picker) {
    private val serverViewModel by activityViewModels<ServerViewModel>()
    private val viewModel by viewModels<MarkerDetailsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dest = requireArguments().getDestination<Destination.UpdateMarker>()
        viewModel.init(dest.markerId, dest.sceneId)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val picker = view.findViewById<Picker>(R.id.picker)
        val sceneTitle = view.findViewById<TextView>(R.id.scene_title)
        val markerTitle = view.findViewById<TextView>(R.id.marker_title)

        viewModel.item.observe(viewLifecycleOwner) { marker ->
            if (marker == null) {
                return@observe
            }

            sceneTitle.text = viewModel.scene.value!!.titleOrFilename
            markerTitle.text =
                "${marker.primary_tag.tagData.name} - ${durationToString(marker.seconds)}"

            val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val skipForward = preferences.getInt("skip_forward_time", 30)
            val skipBack = preferences.getInt("skip_back_time", 10)
            val maxShift = skipBack.coerceAtMost(skipForward)

            val column = PickerColumn()
            column.labelFormat = "%1\$d seconds"
            column.minValue = marker.seconds
                .toInt()
                .coerceAtMost(maxShift) * -1
            column.maxValue = maxShift
            column.currentValue = column.minValue

            picker.separator = ""
            picker.setColumns(listOf(column))
            picker.setColumnValue(0, 0, true)
            picker.isActivated = true
            picker.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                    if (column.currentValue != 0) {
                        val seconds =
                            (marker.seconds + column.currentValue).coerceAtLeast(0.0)
                        val mutationEngine =
                            MutationEngine(StashServer.requireCurrentServer())
                        val result =
                            mutationEngine.updateMarker(
                                SceneMarkerUpdateInput(
                                    id = marker.id,
                                    scene_id = Optional.present(viewModel.scene.value!!.id),
                                    seconds = Optional.present(seconds),
                                ),
                            )
                        Log.v(TAG, "newSeconds=${result?.seconds}")
                    }
                    serverViewModel.navigationManager.goBack()
                }
            }
        }
    }

    companion object {
        private const val TAG = "MarkerPicker"
    }
}
