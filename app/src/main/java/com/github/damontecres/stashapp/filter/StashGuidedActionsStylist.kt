package com.github.damontecres.stashapp.filter

import androidx.leanback.widget.GuidedAction
import androidx.leanback.widget.GuidedActionsStylist
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.filter.picker.GuidedDurationPickerAction
import com.github.damontecres.stashapp.views.DurationPicker

/**
 * Style the [GuidedAction]s for creating a filter. Adds support for additional pickers
 */
class StashGuidedActionsStylist : GuidedActionsStylist() {
    override fun onProvideItemLayoutId(viewType: Int): Int {
        return if (viewType == DURATION_VIEW_TYPE) {
            R.layout.duration_guided_action
        } else {
            return super.onProvideItemLayoutId(viewType)
        }
    }

    override fun getItemViewType(action: GuidedAction): Int {
        return if (action is GuidedDurationPickerAction) {
            DURATION_VIEW_TYPE
        } else {
            return super.getItemViewType(action)
        }
    }

    override fun onBindActivatorView(
        vh: ViewHolder,
        action: GuidedAction,
    ) {
        if (action is GuidedDurationPickerAction) {
            val picker = vh.itemView.findViewById<DurationPicker>(R.id.guidedactions_activator_item)
            picker.duration = action.duration
        } else {
            super.onBindActivatorView(vh, action)
        }
    }

    override fun onUpdateActivatorView(
        vh: ViewHolder,
        action: GuidedAction,
    ): Boolean =
        if (action is GuidedDurationPickerAction) {
            val picker = vh.itemView.findViewById<DurationPicker>(R.id.guidedactions_activator_item)
            if (action.duration != picker.duration) {
                action.duration = picker.duration
                true
            } else {
                false
            }
        } else {
            super.onUpdateActivatorView(vh, action)
        }

    companion object {
        const val DURATION_VIEW_TYPE = 1000
    }
}
