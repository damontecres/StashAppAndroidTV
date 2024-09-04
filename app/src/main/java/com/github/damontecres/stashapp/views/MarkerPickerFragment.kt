package com.github.damontecres.stashapp.views

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.leanback.widget.picker.Picker
import androidx.leanback.widget.picker.PickerColumn
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.MarkerActivity.MarkerDetailsViewModel
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.SceneMarkerUpdateInput
import com.github.damontecres.stashapp.data.Marker
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import kotlinx.coroutines.launch

class MarkerPickerFragment : Fragment(R.layout.marker_picker) {
    private val viewModel by activityViewModels<MarkerDetailsViewModel>()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val marker = requireActivity().intent.getParcelableExtra<Marker>("marker")!!

        val picker = view.findViewById<Picker>(R.id.picker)
        val sceneTitle = view.findViewById<TextView>(R.id.scene_title)
        val markerTitle = view.findViewById<TextView>(R.id.marker_title)

        sceneTitle.text = marker.sceneTitle
        markerTitle.text = "${marker.primaryTagName} - ${durationToString(marker.seconds)}"

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val skipForward = preferences.getInt("skip_forward_time", 30)
        val skipBack = preferences.getInt("skip_back_time", 10)
        val maxShift = skipBack.coerceAtMost(skipForward)

        val column = PickerColumn()
        column.labelFormat = "%1\$d seconds"
        column.minValue = viewModel.seconds.value!!.toInt().coerceAtMost(maxShift) * -1
        column.maxValue = maxShift
        column.currentValue = column.minValue

        picker.separator = ""
        picker.setColumns(listOf(column))
        picker.setColumnValue(0, 0, true)
        picker.isActivated = true
        picker.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler { ex ->
                    Toast.makeText(requireContext(), "Error: ${ex.message}", Toast.LENGTH_LONG)
                },
            ) {
                if (column.currentValue != 0) {
                    val seconds = (viewModel.seconds.value!! + column.currentValue).coerceAtLeast(0.0)
                    val mutationEngine = MutationEngine(requireContext(), true)
                    val result =
                        mutationEngine.updateMarker(
                            SceneMarkerUpdateInput(
                                id = marker.id,
                                scene_id = Optional.present(marker.sceneId),
                                seconds = Optional.present(seconds),
                            ),
                        )
                    Log.v(TAG, "newSeconds=${result?.seconds}")
                    viewModel.seconds.value = result!!.seconds
                }
                requireActivity().supportFragmentManager.popBackStackImmediate()
            }
        }
    }

    companion object {
        private const val TAG = "MarkerPicker"
    }
}
