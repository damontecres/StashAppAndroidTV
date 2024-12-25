package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.util.StashGlide
import com.github.damontecres.stashapp.views.models.GroupViewModel
import com.github.damontecres.stashapp.views.parseTimeToString
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class GroupDetailsFragment : Fragment(R.layout.group_view) {
    private val viewModel: GroupViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private lateinit var frontImage: ImageView
    private lateinit var backImage: ImageView
    private lateinit var table: TableLayout

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        frontImage = view.findViewById(R.id.group_front_image)
        backImage = view.findViewById(R.id.group_back_image)

        table = view.findViewById(R.id.group_table)
        viewModel.item.observe(viewLifecycleOwner) { groupData ->
            if (groupData == null) {
                TODO()
                return@observe
            }

            if (groupData.front_image_path != null) {
                StashGlide
                    .with(requireActivity(), groupData.front_image_path)
                    .error(StashPresenter.glideError(requireContext()))
                    .into(frontImage)
            }
            if (groupData.back_image_path != null) {
                StashGlide
                    .with(requireActivity(), groupData.back_image_path)
                    .error(StashPresenter.glideError(requireContext()))
                    .into(backImage)
            }

            addRow(
                R.string.stashapp_duration,
                groupData.duration?.toDuration(DurationUnit.MINUTES)?.toString(),
            )
            addRow(R.string.stashapp_date, groupData.date)
            addRow(R.string.stashapp_studio, groupData.studio?.name)
            addRow(R.string.stashapp_director, groupData.director)
            addRow(R.string.stashapp_synopsis, groupData.synopsis)
            addRow(R.string.stashapp_created_at, parseTimeToString(groupData.created_at))
            addRow(R.string.stashapp_updated_at, parseTimeToString(groupData.updated_at))
            table.setColumnShrinkable(1, true)
        }
    }

    private fun addRow(
        key: Int,
        value: String?,
    ) {
        if (value.isNullOrBlank()) {
            return
        }
        val keyString = getString(key) + ":"

        val row =
            requireActivity().layoutInflater.inflate(R.layout.table_row, table, false) as TableRow

        val keyView = row.findViewById<TextView>(R.id.table_row_key)
        keyView.text = keyString
        keyView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            resources.getDimension(R.dimen.table_text_size_large),
        )

        val valueView = row.findViewById<TextView>(R.id.table_row_value)
        valueView.text = value
        valueView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            resources.getDimension(R.dimen.table_text_size_large),
        )

        table.addView(row)
    }
}
